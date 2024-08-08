pipeline {
    agent any

    stages {
        stage('Checkout') {
            steps {
                checkout scm
                script {
                    sh("git fetch --tags")
                }
            }
        }
        stage('Find and Run Changed build.sh') {
            steps {
                script {
                    def workspace = env.WORKSPACE
                    def buildDirs = []
                    def lastTag = ''

                    // Try to get the last successful build tag
                    def lastSuccessfulTag = sh(
                        script: "git describe --tags --match 'last-successful-build-*' --abbrev=0",
                        returnStdout: true
                    ).trim()

                    if (lastSuccessfulTag) {
                        lastTag = lastSuccessfulTag
                        echo "Last successful build tag: ${lastSuccessfulTag}"
                    } else {
                        echo "No last successful build tag found, building all build.sh files"
                    }

                    // Find all directories with build.sh
                    def findDirs = sh(
                        script: "find . -name 'build.sh' -exec dirname {} \\;",
                        returnStdout: true
                    ).trim().split("\n")

                    // Check if each directory has changes since lastTag if it exists, otherwise build all
                    findDirs.each { dir ->
                        if (lastTag) {
                            def relativeDirPath = dir.startsWith("./") ? dir.substring(2) : dir
                            def changedFiles = sh(
                                script: "git diff --name-only ${lastTag} -- ${relativeDirPath}",
                                returnStdout: true
                            ).trim()

                            if (changedFiles) {
                                buildDirs << dir
                                echo "Detected changes in ${relativeDirPath}"
                            }
                        } else {
                            buildDirs << dir
                        }
                    }

                    if (buildDirs) {
                        // Create a map to hold parallel stages
                        def parallelStages = [:]

                        // Iterate over the array and create a parallel stage for each build.sh file
                        buildDirs.each { buildDir ->
                            def stageName = buildDir.replaceFirst("./", "")

                            parallelStages[stageName] = {
                                stage(stageName) {
                                    script {
                                        echo "Running build.sh in directory ${buildDir}"
                                        dir(buildDir) {
                                            sh "chmod +x build.sh"
                                            sh "./build.sh"
                                        }
                                    }
                                }
                            }
                        }

                        // Run the parallel stages
                        parallel parallelStages
                    } else {
                        echo 'No changed build.sh file found in the immediate subdirectories since the last successful build.'
                    }

                    // Set a flag if any build.sh was executed
                    currentBuild.description = buildDirs ? 'Build.sh executed' : 'No build.sh executed'

                }
            }
        }
        stage('Clean old tags') {
            steps {
                script {
                    sh """
                        ./clean-old-tags.sh
                    """
                }
            }
        }
    }

    post {
        success {
            script {
                if (currentBuild.description?.contains('Build.sh executed')) {
                    def tagName = "last-successful-build-${env.BUILD_NUMBER}"
                    sh """
                        git tag ${tagName}
                        git push origin ${tagName}
                    """
                } else {
                    echo 'No build.sh was executed, skipping tagging.'
                }
            }
        }
        failure {
            echo 'Build failed, not tagging the commit.'
        }
    }
}
