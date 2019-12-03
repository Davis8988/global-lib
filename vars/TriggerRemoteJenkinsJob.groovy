#!/usr/bin/env groovy

import groovy.json.JsonSlurperClassic 


@NonCPS
def jsonParse(def json) {
    new groovy.json.JsonSlurperClassic().parseText(json)
}


def call(Map args = [:]) {
	/* Assign args */
	jobUrl = args.jobUrl      ?: null
	jobToken = args.jobToken  ?: null
	remoteJenkinsUser = args.remoteJenkinsUser  ?: null
	remoteJenkinsPass = args.remoteJenkinsPass  ?: null
	remoteJenkinsUserToekn = args.remoteJenkinsUserToekn  ?: null
	remoteJobParametersString = args.remoteJobParametersString ?: null
	timeoutSec = args.timeoutSec ?: 120
	sleepBetweenPollingSec = args.sleepBetweenPollingSec ?: 5
	waitForRemoteJobToFinish = args.waitForRemoteJobToFinish       ?: true
	failBuildOnRemoteJobFailure = args.failBuildOnRemoteJobFailure ?: true
	
	/* Validate mandatory args */
	waitForRemoteJobToFinish = waitForRemoteJobToFinish.toBoolean()
	failBuildOnRemoteJobFailure = failBuildOnRemoteJobFailure.toBoolean()
	if (! jobUrl || ! jobToken) {error "Missing mandatory args: \njobUrl=${jobUrl} \njobToken=${jobToken} \n"}
	if (timeoutSec.toInteger() <= 0 || sleepBetweenPollingSec <= 0) {error "Bad args received: \n"+
																			"timeoutSec=${timeoutSec} \n"+
																			"sleepBetweenPollingSec=${sleepBetweenPollingSec} \n"+
																			"timeoutSec, sleepBetweenPollingSec both must be greater than 0"}
	
	/* Print used params in this execution */
	printRemoteJobTriggerParams()
	
	
	/* Fix job url (if needed) */
	jobUrl = jobUrl.trim().replaceAll("/buildWithParameters" , "")
	jobUrl = jobUrl.replaceAll("/build" , "")
	def jobUrlLength = jobUrl.length()
	if (jobUrl[jobUrlLength-1] == '/') {jobUrl=jobUrl.substring(0, jobUrlLength-1)}
	
	remoteJenkinsUrl = jobUrl.substring(0, jobUrl.indexOf("/job")).trim()
	def isCrumbRequired = checkIfCrumbIsRequired(remoteJenkinsUrl)
	if (isCrumbRequired) {
		remoteJenkinsCrumb = getRemoteJenkinsCrumb(remoteJenkinsUrl, remoteJenkinsUser, remoteJenkinsPass, remoteJenkinsUserToekn)
		// if (!remoteJenkinsCrumb) {return}
	}
	
	print "Remote jenkins using crumb: ${remoteJenkinsCrumb}"
	return
	/* Check status & get next-build-number before triggering */
	def abortOnCurlFailure = true
	def remoteJenkinsJobStatus = getRemoteJenkinsJobStatus(jobUrl, abortOnCurlFailure)
	def remoteJenkinsJobStatus_Json = jsonParse(remoteJenkinsJobStatus)
	nextBuildNumber = remoteJenkinsJobStatus_Json.get("nextBuildNumber", null)
	if (! nextBuildNumber) {error "Failed getting next build number in remote jenkins job. \nCannot issue remote command to start a new job"}
	
	/* Trigger remote jenkins job */
	triggerRemoteJenkinsJob(remoteJenkinsJobStatus_Json, jobUrl, jobToken, remoteJobParametersString)
	
	/* If doesn't want to wait - then finish and exit here */
	if (!waitForRemoteJobToFinish) {
		print "Finished triggering build of remote jenkins job: ${jobUrl}/${nextBuildNumber} \nContinuing.."
		return
	} 
	
	/* Wait for it to finish */
	waitForRemoteJenkinsJobToFinish(jobUrl, nextBuildNumber, timeoutSec, sleepBetweenPollingSec)
	/* Delay for 1 seconds to let remote jenkins finish updating */
	sleep(1)

	/* If wants to wait for remote job execution to finish */
	if (waitForRemoteJobToFinish) {
		/* Check if remote job building failed*/
		if (checkIfRemoteJobWasSuccessful(jobUrl, nextBuildNumber) == false) {
			error "Remote job [No. ${nextBuildNumber}] finsihed with failure: ${jobUrl}/${nextBuildNumber}/console" 
		}
	}
	
	print "Remote job [No. ${nextBuildNumber}] finsihed successfully: ${jobUrl}/${nextBuildNumber}/console"
	
}

def printRemoteJobTriggerParams() {
	println "Trigger Remote Jenkins Job Params: \n" +
			" jobUrl=${jobUrl} \n" +
			" jobToken=${jobToken} \n" +
			" waitForRemoteJobToFinish=${waitForRemoteJobToFinish} \n" +
			" failBuildOnRemoteJobFailure=${failBuildOnRemoteJobFailure} \n" +
			" timeoutSec=${timeoutSec} \n" +
			" sleepBetweenPollingSec=${sleepBetweenPollingSec} \n" +
			" remoteJobParametersString=${remoteJobParametersString} \n"
}

def getRemoteJenkinsJobStatus(jobUrl, abortOnCurlFailure) {
	def curl_command = "curl -X POST --fail ${jobUrl}/api/json "
	def proc = curl_command.execute()
	proc.waitFor()
	if (proc.exitValue() && abortOnCurlFailure) {
		error "Failed getting remote jenkins job status.\nCURL execution failure:\n${proc.err.text}"
	} else if (proc.exitValue() && ! abortOnCurlFailure) {
		return "{ \"curl_failed\" : true, \"abortOnCurlFailure\" : false }"
	}
	
	return proc.in.text.trim()
}

def triggerRemoteJenkinsJob(remoteJenkinsJobStatus_Json, jobUrl, jobToken, remoteJobParametersString) {
	
	/* Execution url: */
	def curl_command = "curl -X POST --fail ${jobUrl}/build?token=${jobToken}"
	
	/* If remote job is parameterized, then it has a different remote execution url - so need to overwrite above url */
	def isRemoteJobAcceptsParameters = checkIfRemoteJobAcceptsParameters(remoteJenkinsJobStatus_Json)
	if (isRemoteJobAcceptsParameters) {
		def remoteJobParams = ""
		/* If received params then include them in the request */
		if (remoteJobParametersString) {remoteJobParams = getRemoteJobParametersFormattedString(remoteJobParametersString)}
		
		/* overwrite execution url for parameterized jobs */
		curl_command = "curl -X POST --fail ${jobUrl}/buildWithParameters?token=${jobToken}${remoteJobParams}"
	}
	
	print "Attempting to trigger remote jenkins job"
	def proc = curl_command.execute()
	proc.waitFor()
	if (proc.exitValue()) {
		error "Failed triggering remote jenkins job\nCURL execution failure:\n${proc.err.text}"
	}
}

def waitForRemoteJenkinsJobToFinish(jobUrl, nextBuildNumber, timeoutSeconds, sleepBetweenPollingSec) {
	/* Init */
	def isFinishedWaiting = false
	def abortOnCurlFailure = false  //Should not abort here since on the first few executions that build is not present yet. So we get NOT FOUND error.
	
	/* Wait untill remote job has finished building, or timeout expires*/
	print "Waiting for remote job [No. ${nextBuildNumber}] to start ${jobUrl}/${nextBuildNumber} and finish building.."
	timeout(time: timeoutSeconds, unit: 'SECONDS') {
		while(!isFinishedWaiting) {
			sleep(sleepBetweenPollingSec)
			def remoteJenkinsJobStatus = getRemoteJenkinsJobStatus("${jobUrl}/${nextBuildNumber}", abortOnCurlFailure)
			def remoteJenkinsJobStatus_Json = jsonParse(remoteJenkinsJobStatus)
			isFinishedWaiting = checkIfRemoteJobFinished(remoteJenkinsJobStatus_Json, nextBuildNumber)
		}
	}
	
	print "Done waiting for remote job to finished building.."
}

def checkIfRemoteJobFinished(remoteJenkinsJobStatus_Json, nextBuildNumber) {
	if (remoteJenkinsJobStatus_Json["building"] == null || remoteJenkinsJobStatus_Json["building"] == true) {print "remote job still building"; return false}
	print "remote job finished building";
	return true
}

def checkIfRemoteJobAcceptsParameters(remoteJenkinsJobStatus_Json) {
	for (prop in remoteJenkinsJobStatus_Json.get("property")) {
		if ("${prop._class}" == "hudson.model.ParametersDefinitionProperty") {return true}
	}
	
	return false
	
}

def getRemoteJobParametersFormattedString(remoteJobParametersString) {
	def remoteJobParams_result = ""
	def remoteJobParams_arr = remoteJobParametersString.toString().split(",")
	for (jobParam in remoteJobParams_arr) {
		remoteJobParams_result += "&${jobParam.trim()}"
	}
	/* Replace spaces with %20 for url spacing */
	return remoteJobParams_result.replaceAll(" ", "%20")
}

def checkIfRemoteJobWasSuccessful(jobUrl, nextBuildNumber) {
	/* Read remote job status after done building*/
	def abortOnCurlFailure = true
	remoteJenkinsJobFinishedStatus_Json = jsonParse(getRemoteJenkinsJobStatus(jobUrl, abortOnCurlFailure))
	
	/* Get lastSuccessfulBuild.number property */
	if (! remoteJenkinsJobFinishedStatus_Json) {error "Failed checking if remote job was successful - job result json object is null"}
	def lastSuccessfulBuild = remoteJenkinsJobFinishedStatus_Json.get("lastSuccessfulBuild", null)
	if (! lastSuccessfulBuild) {error "Failed checking if remote job was successful - could not read property 'lastSuccessfulBuild' of job result json object"}
	
	/* Check if lastSuccessfulBuild number equals to nextBuildNumber */
	if (lastSuccessfulBuild.number == nextBuildNumber) {
		return true
	} else {
		return false
	}
}


def checkIfCrumbIsRequired(remoteJenkinsUrl) {
	print "Remote Jenkins URL: ${remoteJenkinsUrl}"
	def curl_command = "curl ${remoteJenkinsUrl}/api/json"
	def proc = curl_command.execute()
	proc.waitFor()
	if (proc.exitValue()) {
		error "Failed checking if remote jenkins: ${remoteJenkinsUrl} requires valid crumb for remote triggering job via api requests\nError:\n${proc.err.text}"
	}
	
	def remoteJenkinsStatus_Json = jsonParse(proc.in.text)
	
	return remoteJenkinsStatus_Json.useCrumbs == true
}


def getRemoteJenkinsCrumb(remoteJenkinsUrl, remoteJenkinsUser, remoteJenkinsPass, remoteJenkinsUserToekn) {
	if(!remoteJenkinsUser) {return null}
	def curl_command = ""
	if (remoteJenkinsPass) { 
		curl_command = "curl --fail --silent --cookie-jar /tmp/cookies ${remoteJenkinsUrl}/crumbIssuer/api/json -u ${remoteJenkinsUser}:${remoteJenkinsPass}"
	} else if (remoteJenkinsUserToekn) {
		curl_command = "curl --fail --silent --cookie-jar /tmp/cookies ${remoteJenkinsUrl}/crumbIssuer/api/json -u ${remoteJenkinsUser}:${remoteJenkinsUserToekn}"
	} else {
		return null
	}
	
	
	sh '''
		export remoteJenkinsUrl=http://13.90.250.12:8081
		export remoteJenkinsUser=admin
		export remoteJenkinsPass=Abcd1234
		export COOKIE_JAR=/tmp/cookies



		JENKINS_CRUMB=$(curl --fail --silent --cookie-jar $COOKIE_JAR $remoteJenkinsUrl'/crumbIssuer/api/xml?xpath=concat(//crumbRequestField,":",//crumb)' -u $remoteJenkinsUser:$remoteJenkinsPass)
		echo $JENKINS_CRUMB

		curl -X POST --fail --cookie $COOKIE_JAR -u $remoteJenkinsUser:$remoteJenkinsPass -H $JENKINS_CRUMB  $remoteJenkinsUrl/job/Test_Remote_Trigger/build

		echo finished

	'''
	
	
	return
	def proc = curl_command.execute()
	proc.waitFor()
	if (proc.exitValue()) {
		error "Failed getting valid crumb from remote jenkins: ${remoteJenkinsUrl} \nError:\n${proc.err.text}"
	}
	
	def curlOutput_json = jsonParse(proc.in.text)
	def validCrumb = curlOutput_json.crumb
	def crumbRequestField = curlOutput_json.crumbRequestField
	print "crumbRequestField: validCrumb = ${crumbRequestField}: ${validCrumb}"
	
	curl_command = "curl -X POST --fail --cookie-jar /tmp/cookies -u ${remoteJenkinsUser}:${remoteJenkinsPass} -H ${crumbRequestField}:${validCrumb}  ${remoteJenkinsUrl}/job/Test_Remote_Trigger/build"
	
	print "Executing: " + curl_command.toString()
	proc = curl_command.execute()
	proc.waitFor()
	if (proc.exitValue()) {
		error "Failed executing with valid crumb \nError:\n${proc.err.text}"
	}
	
	print "Result: ${proc.in.text}"
	
	return validCrumb
}

