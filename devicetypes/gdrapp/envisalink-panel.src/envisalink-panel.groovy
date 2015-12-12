/**
 *  Envisalink Panel
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
	definition (name: "Envisalink Panel", namespace: "gdrapp", author: "Greg Rapp") {
		capability "Polling"
		capability "Refresh"
        
        attribute "battery", "enum", ["trouble", "restored"]
        attribute "ac", "enum", ["trouble", "restored"]
        attribute "bell", "enum", ["trouble", "restored"]
        attribute "generalTamper", "enum", ["tamper", "restored"]
        attribute "fireTrouble", "enum", ["trouble", "restored"]
	}

	simulator {
		// TODO: define status and reply messages here
	}

	tiles(scale:2) {
        standardTile("battery", "device.battery", width: 2, height: 2, canChangeBackground: true, canChangeIcon: true) {
          state "restored", label: 'Batt OK', backgroundColor: "#79b821", icon:"st.Appliances.appliances17"
          state "trouble", label: 'Batt Trouble', backgroundColor: "#ff0000", icon:"st.Appliances.appliances17"
        }
        standardTile("ac", "device.ac", width: 2, height: 2, canChangeBackground: true, canChangeIcon: true) {
          state "restored", label: 'AC OK', backgroundColor: "#79b821", icon:"st.Appliances.appliances17"
          state "trouble", label: 'AC Trouble', backgroundColor: "#ff0000", icon:"st.Appliances.appliances17"
        }
        standardTile("bell", "device.bell", width: 2, height: 2, canChangeBackground: true, canChangeIcon: true) {
          state "restored", label: 'Bell OK', backgroundColor: "#79b821", icon:"st.Office.office6"
          state "trouble", label: 'Bell Trouble', backgroundColor: "#ff0000", icon:"st.Office.office6"
        }
        standardTile("generalTamper", "device.generalTamper", width: 2, height: 2, canChangeBackground: true, canChangeIcon: true) {
          state "restored", label: 'Tamper OK', backgroundColor: "#79b821", icon:"st.Home.home3"
          state "tamper", label: 'Tamper Trouble', backgroundColor: "#ff0000", icon:"st.Home.home3"
        }
        standardTile("fireTrouble", "device.fireTrouble", width: 2, height: 2, canChangeBackground: true, canChangeIcon: true) {
          state "restored", label: 'Fire OK', backgroundColor: "#79b821", icon:"st.Home.home29"
          state "trouble", label: 'Fire Trouble', backgroundColor: "#ff0000", icon:"st.Home.home29"
        }
        standardTile("refresh", "device.refresh", inactiveLabel: false, decoration: "flat", width: 2, height: 2) {
          state "default", action:"refresh.refresh", icon:"st.secondary.refresh"
    	}
        main("ac")
        details(["ac", "battery", "bell", "generalTamper", "fireTrouble", "refresh"])
	}
}

// parse events into attributes
def parse(String description) {
	log.debug "Parsing '${description}'"

}

def handleUpdate(cmd, payload) {
	log.debug "Received panel update request [cmd=$cmd,payload=$payload]"
    def event,value,description = null
    switch (cmd) {
	case "800":
          event="battery"
          value="trouble"
          description="Panel Has Battery Trouble"
          break
        case "801":
          event="battery"
          value="restored"
          description="Panel Battery Trouble Restored"
          break
	case "802":
          event="ac"
          value="trouble"
          description="Panel Has AC Power Trouble"
          break
        case "803":
          event="ac"
          value="restored"
          description="Panel AC Power Trouble Restored"
          break
	case "806":
          event="bell"
          value="trouble"
          description="Panel Has Bell Trouble"
          break
        case "807":
          event="bell"
          value="restored"
          description="Panel Bell Trouble Restored"
          break
	case "829":
          event="generalTamper"
          value="tamper"
          description="Panel Has A General Tamper Trouble"
          break
        case "830":
          event="generalTamper"
          value="restored"
          description="Panel General Tamper Trouble Restored"
          break
	case "842":
          event="fireTrouble"
          value="trouble"
          description="Panel Has A Fire Alarm Trouble"
          break
        case "843":
          event="fireTrouble"
          value="restored"
          description="Panel Fire Alarm Trouble Restored"
          break
    }
    if (event && value) {
      log.debug "Sending event [event=$event, value=$value, descriptionText=$description]"
      sendEvent(name: event, value: value, descriptionText: description)
    }
}

// handle commands
def poll() {
	log.debug "Executing 'poll'"
    parent.pollEnvisalink()
}

def refresh() {
	log.debug "Executing 'refresh'"
    parent.pollEnvisalink()
}
