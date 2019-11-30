#!/usr/bin/env groovy

import groovy.json.JsonSlurperClassic 


@NonCPS
def jsonParse(def json) {
    new groovy.json.JsonSlurperClassic().parseText(json)
}


def call(Map args = [:]) {
	arg_remoteJenkinsJobUrl = args.jobUrl
	arg_remoteJenkinsJobToken = args.token
	
	println "Received args: \n" +
			"jobUrl=${arg_remoteJenkinsJobUrl} \n" +
			"token=${arg_remoteJenkinsJobToken} \n" 
	
	/* Check mandatory args */
	if (! arg_remoteJenkinsJobUrl || ! arg_remoteJenkinsJobToken) {
		error "Missing mandatory args: \n" +
			  "jobUrl=${arg_remoteJenkinsJobUrl} \n" +
			  "token=${arg_remoteJenkinsJobToken} \n"
	}
	
	
			
	def remoteJenkinsJobUrl_Corrected = getCorrectRemoteJenkinsJobUrl(arg_remoteJenkinsJobUrl)
	def remoteJenkins_Status = getRemoteJenkinsStatus()
	def params = jsonParse(env.choice_app)
}


def getCorrectRemoteJenkinsJobUrl(arg_remoteJenkinsJobUrl) {
	remoteJenkinsJobUrl_Corrected = arg_remoteJenkinsJobUrl.trim().replaceAll("/buildWithParameters" , "")
	remoteJenkinsJobUrl_Corrected = remoteJenkinsJobUrl_Corrected.replaceAll("/build" , "")
	
	def strLength = remoteJenkinsJobUrl_Corrected.length()
	if (remoteJenkinsJobUrl_Corrected[strLength-1] == '/') {remoteJenkinsJobUrl_Corrected=remoteJenkinsJobUrl_Corrected.substring(0, strLength-1)}
	
	return remoteJenkinsJobUrl_Corrected
}


