/**
 *  Envisalink Contact Zone
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
metadata {
	definition (name: "Envisalink Contact Zone", namespace: "gdrapp", author: "Greg Rapp") {
		capability "Contact Sensor"
		capability "Refresh"
        
        attribute "alarm", "enum", ["alarm", "restored"]
        attribute "tamper", "enum", ["tamper", "restored"]
        attribute "fault", "enum", ["fault", "restored"]
	}

	simulator {
		// TODO: define status and reply messages here
	}

	tiles(scale:2) {
        standardTile("contact", "device.contact", width: 2, height: 2, canChangeBackground: true, canChangeIcon: true) {
          state "unknown",label: 'Unknown', icon: "st.contact.contact.open", backgroundColor: "#ffa81e"
          state "open", label: 'Open', icon: "st.contact.contact.open", backgroundColor: "#ffa81e"
          state "closed", label: 'Closed', icon: "st.contact.contact.closed", backgroundColor: "#79b821"
        }
        standardTile("alarm", "device.alarm", width: 2, height: 2, canChangeBackground: true, canChangeIcon: true) {
          state "restored", label: 'No Alarm', icon: "st.contact.contact.closed", backgroundColor: "#79b821"
          state "alarm", label: 'Alarm', icon: "st.contact.contact.open", backgroundColor: "#ff0000"
        }
        standardTile("tamper", "device.tamper", width: 2, height: 2, canChangeBackground: true, canChangeIcon: true) {
          state "restored", label: 'No Tamper', icon: "st.contact.contact.closed", backgroundColor: "#79b821"
          state "tamper", label: 'Tamper', icon: "st.contact.contact.open", backgroundColor: "#ff0000"
        }
        standardTile("fault", "device.fault", width: 2, height: 2, canChangeBackground: true, canChangeIcon: true) {
          state "restored", label: 'No Fault', icon: "st.contact.contact.closed", backgroundColor: "#79b821"
          state "fault", label: 'Fault', icon: "st.contact.contact.open", backgroundColor: "#ff0000"
        }
        standardTile("refresh", "device.refresh", inactiveLabel: false, decoration: "flat", width: 2, height: 2) {
          state "default", action:"refresh.refresh", icon:"st.secondary.refresh"
    	}
        
        main("contact")
        details(["contact","alarm","tamper","fault","refresh"])
	}
}

// parse events into attributes
def parse(String description) {
	log.debug "Parsing '${description}'"
	// TODO: handle 'contact' attribute

}

def handleUpdate(cmd, payload) {
	log.debug "Received contact zone update request [cmd=$cmd,payload=$payload]"
    def event,value,description = null
    switch (cmd) {
		case "601":
        	event = "alarm"
        	value = "alarm"
            description = "Contact Alarm Detected"
        	break
        case "602":
        	event = "alarm"
        	value = "alarmRestore"
            description = "Contact Alarm Restored"
        	break
		case "603":
        	event = "tamper"
        	value = "tamper"
            description = "Contact Tampered Detected"
        	break
        case "604":
        	event = "tamper"
        	value = "tamperRestore"
            description = "Contact Tamper Restored"
        	break
		case "605":
        	event = "fault"
        	value = "fault"
            description = "Contact Fault Detected"
        	break
        case "606":
        	event = "fault"
        	value = "faultRestore"
            description = "Contact Fault Restored"
        	break
		case "609":
        	event = "contact"
        	value = "open"
            description = "Contact Is Open"
        	break
        case "610":
        	event = "contact"
        	value = "closed"
            description = "Contact Is Closed"
        	break
    	default:
        	event = "contact"
        	value = "unknown"
            description = "Contact Status Is Unknown"
	}
    log.debug "Sending event [event=$event, value=$value, descriptionText: $description]"
    sendEvent(name: event, value: value, descriptionText: description)
}

// handle commands
def refresh() {
	log.debug "Executing 'refresh'"
    parent.pollEnvisalink()
}


