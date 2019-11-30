#!/usr/bin/env groovy

import groovy.json.JsonSlurperClassic 


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
	
	
	def remoteJenkins_Status = getRemoteJenkinsStatus(jobUrl)
	def remoteJenkins_Status_Json = jsonParse(remoteJenkins_Status)
	print("Remote Jenkins Status:\n"+remoteJenkins_Status_Json)
}


def getRemoteJenkinsStatus(jobUrl) {
	def curl_command = 'curl -X POST "${jobUrl}/api/json"'
	def proc = curl_command.execute()
	proc.waitFor()
	if (proc.exitValue()) {
		error "${proc.err.text}"
	}
	
	return proc.in.text.trim()
}


