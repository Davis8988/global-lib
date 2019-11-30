#!/usr/bin/env groovy

import groovy.json.JsonSlurperClassic 


@NonCPS
def jsonParse(def json) {
    new groovy.json.JsonSlurperClassic().parseText(json)
}


def call(Map args = [:]) {
	checkArgs(args)
	
	arg_remoteJenkinsJobUrl = args.jobUrl
	arg_remoteJenkinsJobToken = args.token
	
	
	def remoteJenkinsJobUrl_Corrected = getCorrectRemoteJenkinsJobUrl(arg_remoteJenkinsJobUrl)
	def remoteJenkins_Status = getRemoteJenkinsStatus(remoteJenkinsJobUrl_Corrected, arg_remoteJenkinsJobToken)
	def params = jsonParse(env.choice_app)
}

def checkArgs(args) {
	/* Check mandatory args */
	if (! args.arg_remoteJenkinsJobUrl || ! args.arg_remoteJenkinsJobToken) {
		error "Missing mandatory args: \n" +
			  "jobUrl=${args.arg_remoteJenkinsJobUrl} \n" +
			  "token=${args.arg_remoteJenkinsJobToken} \n"
	}
	
	println "Received args: \n" +
			"jobUrl=${arg_remoteJenkinsJobUrl} \n" +
			"token=${arg_remoteJenkinsJobToken} \n" 
}


def getCorrectRemoteJenkinsJobUrl(arg_remoteJenkinsJobUrl) {
	remoteJenkinsJobUrl_Corrected = arg_remoteJenkinsJobUrl.trim().replaceAll("/buildWithParameters" , "")
	remoteJenkinsJobUrl_Corrected = remoteJenkinsJobUrl_Corrected.replaceAll("/build" , "")
	
	def strLength = remoteJenkinsJobUrl_Corrected.length()
	if (remoteJenkinsJobUrl_Corrected[strLength-1] == '/') {remoteJenkinsJobUrl_Corrected=remoteJenkinsJobUrl_Corrected.substring(0, strLength-1)}
	
	return remoteJenkinsJobUrl_Corrected
}

def getRemoteJenkinsStatus(remoteJenkinsJobUrl_Corrected, arg_remoteJenkinsJobToken) {
	
	sh 'curl -X POST "${remoteJenkinsJobUrl_Corrected}/build?token=${arg_remoteJenkinsJobToken}"'
	
}


