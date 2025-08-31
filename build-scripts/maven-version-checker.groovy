#!/usr/bin/env groovy

import groovy.xml.XmlSlurper

import java.nio.file.Paths
import java.util.concurrent.TimeUnit

String runGitCommand(String command, File commandExecutionDirectory = null) {
    def os = System.getProperty('os.name').toLowerCase()
    Process proc
    StringBuilder output = new StringBuilder()
    StringBuilder error = new StringBuilder()
    if (os.contains('win')) {
        System.out.println("Running on windows: ${command} in directory ${commandExecutionDirectory}")
        if (commandExecutionDirectory != null) {
            proc = ["cmd.exe", "/c", command].execute(null, commandExecutionDirectory)
        } else {
            proc = ["cmd.exe", "/c", command].execute()
        }
    } else {
        System.out.println("Running in shell: ${command} in directory ${commandExecutionDirectory}")
        if (commandExecutionDirectory != null) {
            proc = ["sh", "-c", command].execute(null, commandExecutionDirectory)
        } else {
            proc = ["sh", "-c", command].execute()
        }
    }
    proc.consumeProcessOutput(output, error)
    proc.waitFor(10, TimeUnit.SECONDS)

    if (proc.exitValue() != 0) {
        def errorMessage = "ERROR: Git command failed."
        if (error != null && !error.isEmpty()) {
            errorMessage += "\nError output: ${error}"
        }
        if (output != null && !output.isEmpty()) {
            errorMessage += "\nOutput: ${output}"
        }
        System.err.println(errorMessage)
        System.exit(1)
    }
    return output.toString()
}

def extractVersion(String pomContent) {
    def pomXml = new XmlSlurper().parseText(pomContent)
    // Return the first <version> tag that is not inside <parent>
    def parentVersion = pomXml.parent?.version?.text()
    def projectVersion = pomXml.version?.text()
    if (projectVersion && (!parentVersion || projectVersion != parentVersion)) {
        return projectVersion
    }
    return null
}

String mainPomFile(File projectBaseDir) {
    String baseDirName = projectBaseDir.getName()
    return "${baseDirName}/pom.xml".toString()
}

def gitBaseDir() {
    def topDir = runGitCommand("git rev-parse --show-toplevel").trim()
    if (topDir.isEmpty()) {
        System.err.println("ERROR: Could not find the top most directory for this git repo")
        System.exit(1)
    }
    return Paths.get(topDir).toFile()
}

def getPreviousCommitHash(File projectBaseDir, File gitBaseDir) {
    def pomFile = mainPomFile(projectBaseDir)
    def command = "git log -1 --skip 1 --format=%H -- ${pomFile}"
    def commitHash = runGitCommand(command, gitBaseDir).trim()

    if (commitHash.isEmpty()) {
        System.err.println("ERROR: Could not find previous commit that modified ${pomFile}")
        System.exit(1)
    }

    return commitHash
}

def currentCommitHash(File gitBaseDir) {
    def command = "git log -1 --format=%H"
    return runGitCommand(command, gitBaseDir).trim()
}

def "find files changed under project directory between two commit hash"(File projectBaseDir, File gitBaseDir,
                                                                         String previousCommitHash, String currentCommitHash) {
    def projectPath = projectBaseDir.toPath().toAbsolutePath()
    def command = "git diff --name-only ${previousCommitHash}^ ${currentCommitHash}"
    def changedFiles = runGitCommand(command, gitBaseDir)

    return changedFiles.readLines()
            .collect { it.trim() }
            .findAll { !it.isEmpty() }
            .collect { Paths.get(it) }
            .findAll { path ->
                try {
                    def absolute = gitBaseDir.toPath().resolve(path).toAbsolutePath()
                    absolute.startsWith(projectPath)
                } catch (Exception e) {
                    false
                }
            }
            .collect { it.toString().replaceAll('\\\\', '/') }
}

def "check if pom was changed"(File projectBaseDir, File gitBaseDir) {
    def prevHash = getPreviousCommitHash(projectBaseDir, gitBaseDir)
    def currHash = currentCommitHash(gitBaseDir)
    def changedFiles = "find files changed under project directory between two commit hash"(projectBaseDir, gitBaseDir, prevHash, currHash)
    changedFiles.each { file ->
        println "Changed file: ${file}"
    }
    var pomFile = mainPomFile(projectBaseDir)
    println "pom file: ${pomFile}"
    if (!changedFiles.any {
        it.trim() == pomFile
    }) {
        System.err.println('ERROR: pom.xml was not changed in the latest commit. Version must be updated.')
        System.exit(1)
    }
}

def "check if version was changed"(baseDir) {
    String pomHead = runGitCommand("git --no-pager show HEAD:" + mainPomFile(baseDir))
    String pomPrev = runGitCommand("git --no-pager show HEAD~1:" + mainPomFile(baseDir))
    def versionHead = extractVersion(pomHead)
    def versionPrev = extractVersion(pomPrev)
    if (!versionHead || !versionPrev) {
        System.err.println('ERROR: Could not extract <version> from pom.xml in one of the commits.')
        System.exit(1)
    }
    if (versionHead == versionPrev) {
        System.err.println('ERROR: pom.xml <version> was not updated in the latest commit. Version must be incremented.')
        System.exit(1)
    }
    System.out.println(new String("pom.xml version updated: $versionPrev -> $versionHead"))
}

def "check for uncommitted changes"() {
    def status = runGitCommand('git status --porcelain')
    if (status && !status.trim().isEmpty()) {
        System.err.println('ERROR: You have uncommitted changes. Please commit or stash them before running this script.')
        System.exit(1)
    }
}

/* resolve project base directory through maven */
File projectBaseDir = this['project']['basedir']
println "Project base directory: ${projectBaseDir}"

File gitBaseDir = gitBaseDir()
println "Git base directory: ${gitBaseDir}"

System.out.println "Checking for uncommited changes"
"check for uncommitted changes"()
System.out.println "Going to check if pom.xml has changed"
"check if pom was changed"(projectBaseDir, gitBaseDir)
System.out.println "Going to check if version tag is updated in pom.xml"
"check if version was changed"(projectBaseDir)