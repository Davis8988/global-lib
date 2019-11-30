#!/usr/bin/env groovy

import groovy.json.JsonSlurperClassic 
import groovy.json.JsonOutput

@NonCPS
def jsonParse(def json) {
    new groovy.json.JsonSlurperClassic().parseText(json)
}


def call(Map args = [:]) {
	/* Assign args */
	jobUrl = args.jobUrl  ?: null
	jobToken = args.jobToken ?: null
	timeoutSeconds = args.timeoutSeconds ?: 30
	
	/* Check mandatory args */
	if (! jobUrl || ! jobToken) {error "Missing mandatory args: \njobUrl=${jobUrl} \njobToken=${jobToken} \n"}
	if (timeoutSeconds.toInteger() <= 0) {error "Bad args received: \ntimeoutSeconds=${timeoutSeconds} \ntimeoutSeconds must be greater than 0"}
	
	println "Trigger Remote Jenkins Job Params: \n" +
			" jobUrl=${jobUrl} \n" +
			" jobToken=${jobToken} \n" 
			" timeoutSeconds=${timeoutSeconds}"
	
	
	/* Fix job url (if needed) */
	jobUrl = jobUrl.trim().replaceAll("/buildWithParameters" , "")
	jobUrl = jobUrl.replaceAll("/build" , "")
	def jobUrlLength = jobUrl.length()
	if (jobUrl[jobUrlLength-1] == '/') {jobUrl=jobUrl.substring(0, jobUrlLength-1)}
	
	
	def remoteJenkinsJobStatus = getRemoteJenkinsJobStatus(jobUrl)
	def remoteJenkinsJobStatus_Json = jsonParse(remoteJenkinsJobStatus)
	
	nextBuildNumber = remoteJenkinsJobStatus_Json.get("nextBuildNumber", null)
	if (! nextBuildNumber) {error "Failed getting next build number in remote jenkins job. \nCannot issue remote command to start a new job"}
	
	executeRemoteJenkinsJob(jobUrl, jobToken)
	
	waitForRemoteJenkinsJobToFinish(jobUrl, nextBuildNumber, timeoutSeconds)
	
}


def getRemoteJenkinsJobStatus(jobUrl) {
	def curl_command = "curl -X POST --fail ${jobUrl}/api/json "
	print "Executing: ${curl_command}"
	def proc = curl_command.execute()
	proc.waitFor()
	if (proc.exitValue()) {
		error "Failed getting remote jenkins job status.\nCURL execution failure:\n${proc.err.text}"
	}
	
	return proc.in.text.trim()
}

def executeRemoteJenkinsJob(jobUrl, jobToken) {
	def curl_command = "curl -X POST --fail ${jobUrl}/build?token=${jobToken}"
	print "Execution remote jenkins job at: ${jobUrl}/build"
	def proc = curl_command.execute()
	proc.waitFor()
	if (proc.exitValue()) {
		error "Failed starting remote jenkins job\nCURL execution failure:\n${proc.err.text}"
	}
}


def waitForRemoteJenkinsJobToFinish(jobUrl, nextBuildNumber, timeoutSeconds) {
	def isFinishedWaiting = false
	timeout(time: timeoutSeconds, unit: 'SECONDS') {
		while(!isFinishedWaiting) {
			sleep(timeoutSeconds)
			def remoteJenkinsJobStatus = getRemoteJenkinsJobStatus("${jobUrl}/${nextBuildNumber}")
			isFinishedWaiting = checkIfRemoteJobFinished(jsonParse(remoteJenkinsJobStatus), nextBuildNumber)
		}
	}
	
	
	print "Done waiting for remote job to finished building"
}

def checkIfRemoteJobFinished(remoteJenkinsJobStatus_Json, nextBuildNumber) {
	if (remoteJenkinsJobStatus_Json['building']) {print "remote job still building"; return false}
	print "remote job finished building";
	return true
}


def checkIfCSRL(jobUrl) {
	def curl_command = "curl -X POST ${jobUrl}/api/json "
	print "Executing: ${curl_command}"
	def proc = curl_command.execute()
	proc.waitFor()
	if (proc.exitValue()) {
		error "CURL execution failed:\n${proc.err.text}"
	}
	
	return proc.in.text.trim()
}


