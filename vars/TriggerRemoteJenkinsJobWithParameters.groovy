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
	
	/* Check mandatory args */
	if (! jobUrl || ! jobToken) {
		error "Missing mandatory args: \n" +
			  "jobUrl=${jobUrl} \n" +
			  "jobToken=${jobToken} \n"
	}
	
	println "Received args: \n" +
			"jobUrl=${jobUrl} \n" +
			"jobToken=${jobToken} \n" 
	
	
	/* Fix job url (if needed) */
	jobUrl = jobUrl.trim().replaceAll("/buildWithParameters" , "")
	jobUrl = jobUrl.replaceAll("/build" , "")
	def jobUrlLength = jobUrl.length()
	if (jobUrl[jobUrlLength-1] == '/') {jobUrl=jobUrl.substring(0, jobUrlLength-1)}
	
	
	def remoteJenkinsJobStatus = getRemoteJenkinsJobStatus(jobUrl)
	def remoteJenkinsJobStatus_Json = jsonParse(remoteJenkinsJobStatus)
	
	nextBuildNumber = remoteJenkinsJobStatus_Json.get("nextBuildNumber", null)
	nextBuildNumber2 = remoteJenkinsJobStatus_Json.get("nextBuildNumber2", null)
	print("Next build numbers:\n" +
		  " nextBuildNumber=${nextBuildNumber}\n "+
		  " nextBuildNumber2=${nextBuildNumber2}\n ")
		  
	
	
}


def getRemoteJenkinsJobStatus(jobUrl) {
	def curl_command = "curl -X POST --fail ${jobUrl}/api/json "
	print "Executing: ${curl_command}"
	def proc = curl_command.execute()
	proc.waitFor()
	if (proc.exitValue()) {
		error "CURL execution failed:\n${proc.err.text}"
	}
	
	return proc.in.text.trim()
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


