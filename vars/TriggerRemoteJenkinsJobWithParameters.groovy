#!/usr/bin/env groovy

import groovy.json.JsonSlurperClassic 


@NonCPS
def jsonParse(def json) {
    new groovy.json.JsonSlurperClassic().parseText(json)
}


def call(Map[:] args) {
	remoteJenkinsJobUrl_org = args.jobUrl ? null
	remoteJenkinsJobToken = args.token ? null
	
	/* Check mandatory params */
	if (! remoteJenkinsJobUrl_org || !remoteJenkinsJobToken) {
		error "Missing mandatory params: \n" +
			  "jobUrl=${remoteJenkinsJobUrl_org} \n" +
			  "token=${remoteJenkinsJobToken} \n"
	}
	
	println "Received args: \n" +
			"FName=${FName} \n" +
			"LName=${LName} \n" 
			
	def remoteJenkinsJobUrl_Corrected = getCorrectRemoteJenkinsJobUrl()
	def params = jsonParse(env.choice_app)
}


