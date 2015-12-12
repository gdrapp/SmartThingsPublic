/**
 *  Contact Open Alert
 *
 *  Copyright 2015 Greg Rapp
 *
 *  Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 *  in compliance with the License. You may obtain a copy of the License at:
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software distributed under the License is distributed
 *  on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License
 *  for the specific language governing permissions and limitations under the License.
 *
 */
definition(
    name: "Contact Open Alert",
    namespace: "gdrapp",
    author: "Greg Rapp",
    description: "Send a notification if a contact is open at a specific time.",
    category: "Safety & Security",
    iconUrl: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience.png",
    iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png",
    iconX3Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png")


preferences {
	section("Select contact(s)") {
		input("contactsToCheck", "capability.contactSensor", title: "Which contacts?", multiple: true, required: true)
	}
    section("Select time") {
    	input("timeToCheck", "time", title: "What time?", required: true)
    }
    section("Notifications") {
        input("notifyRecipients", "contact", title: "Send notifications to") {
	        input("notifyPush", "bool", title: "Notify with push notification?", required: false)
            input("notifyPhone", "phone", title: "Notify with text message?", description: "Phone Number", required: false)
        }
    }
}

def installed() {
	log.debug "Installed with settings: ${settings}"
	initialize()
}

def updated() {
	log.debug "Updated with settings: ${settings}"
	unschedule()
	initialize()
}

def initialize() {
	schedule(timeToCheck, doCheck)
}

def doCheck() {
	contactsToCheck.each {
    	def contactState = it.currentValue("contact")
    	if (contactState != "closed") {
        	def message = "${it.displayName} is open"
        	if (location.contactBookEnabled && notifyRecipients) {
               	log.debug "Sending notification to $notifyRecipients with message: $message"
                sendNotificationToContacts(message, notifyRecipients)
            } else {
                if (notifyPhone) {
                	log.debug "Sending notification via SMS to $notifyPhone with message: $message"
                    sendSms(notifyPhone, message)
                } else {
                	log.error "Unable send SMS notification message because a phone number was not configured"
                }
                
                if (notifyPush) {
                	log.debug "Sending notification via push with message: $message"
                	sendPush(message)
                } else {
                	log.verbose "Push notification disabled"
                }
            }            
        }
    }
}