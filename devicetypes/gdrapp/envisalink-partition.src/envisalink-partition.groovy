/**
 *  Envisalink Partition
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
	definition (name: "Envisalink Partition", namespace: "gdrapp", author: "Greg Rapp") {
		capability "Refresh"
        
		command "arm"
		command "armStay"
		command "armAway"
		command "armZeroEntryDelay"
		command "disarm"
	}

	simulator {
		// TODO: define status and reply messages here
	}

	tiles(scale:2) {
        multiAttributeTile(name:"partition", type:"generic", width:6, height:4) {
	      tileAttribute("device.partition", key: "PRIMARY_CONTROL") {
            attributeState "unknown", label: 'Unknown', backgroundColor: "#79b821", icon:"st.Home.home2"
            attributeState "armed", label: 'Armed', action: "disarm", nextState: "disarming", backgroundColor: "#ff0000", icon:"st.Home.home3"
            attributeState "armedAway", label: 'Away', action: "disarm", nextState: "disarming", backgroundColor: "#ff0000", icon:"st.Home.home3"
            attributeState "armedStay", label: 'Stay', action: "disarm",  nextState: "disarming", backgroundColor: "#ff0000", icon:"st.Home.home3"
            attributeState "armedZeroEntryAway", label: 'Zero Entry', action: "disarm", nextState: "disarming", backgroundColor: "#ff0000", icon:"st.Home.home3"
            attributeState "armedZeroEntryStay", label: 'Zero Entry', action: "disarm", nextState: "disarming", backgroundColor: "#ff0000", icon:"st.Home.home3"
            attributeState "arming", label: 'Arming', action: "", backgroundColor: "#ff9900", icon:"st.Home.home3"
            attributeState "disarming", label: 'Disarming', action: "", backgroundColor: "#ff9900", icon:"st.Home.home3"
            attributeState "exitDelay", label: 'Exit', action: "disarm", nextState: "disarming", backgroundColor: "#ff9900", icon:"st.Home.home3"
            attributeState "entryDelay",label: 'Entry', action: "disarm", nextState: "disarming", backgroundColor: "#ff9900", icon:"st.Home.home3"
            attributeState "disarmed", label: 'Disarmed', action: "arm", nextState: "arming", backgroundColor: "#79b821", icon:"st.Home.home2"
            attributeState "notReady", label: 'Open', backgroundColor: "#ff9900", icon:"st.Home.home2"
            attributeState "keypadLockout", label: 'Keypad Lck', backgroundColor: "#ff9900", icon:"st.Home.home2"
            attributeState "ready", label: 'Ready', action: "arm", nextState: "arming", backgroundColor: "#79b821", icon:"st.Home.home2"
            attributeState "readyForceArming", label: 'Ready Force', action: "arm", nextState: "arming", backgroundColor: "#79b821", icon:"st.Home.home2"
            attributeState "alarm", label: 'Alarm', action: "disarm", nextState: "disarming", backgroundColor: "#ff0000", icon:"st.Home.home3"    
		  }
        }   
        standardTile("armStay", "device.armStay", inactiveLabel: false, decoration: "flat", width: 2, height: 2) {
          state "default", label:"Arm Stay", action:"armStay", icon:"st.Home.home4"
    	}
        standardTile("armAway", "device.armAway", inactiveLabel: false, decoration: "flat", width: 2, height: 2) {
          state "default", label:"Arm Away", action:"armAway", icon:"st.Home.home3"
    	}
        standardTile("armZeroEntryDelay", "device.armZeroEntryDelay", inactiveLabel: false, decoration: "flat", width: 2, height: 2) {
          state "default", label:"Arm No Delay", action:"armZeroEntryDelay", icon:"st.Home.home4"
    	}
        standardTile("refresh", "device.refresh", inactiveLabel: false, decoration: "flat", width: 2, height: 2) {
          state "default", action:"refresh.refresh", icon:"st.secondary.refresh"
    	}
        
        main("partition")
        details(["partition","armStay","armAway","armZeroEntryDelay","refresh"])
	}
}

// parse events into attributes
def parse(String description) {
	log.debug "Parsing '${description}'"
}

def handleUpdate(cmd, payload) {
	log.debug "Received partition update request [cmd=$cmd,payload=$payload]"
    def value,description = null
    switch (cmd) {
		case "650":
        	value = "ready"
            description = "Partition Is Ready"
        	break
        case "651":
        	value = "notReady"
            description = "Partition Is Not Ready"
        	break
        case "652":
        	if (payload.length() == 2) {
                switch (payload[1]) {
                	case "0":
                    	value = "armedAway"
			            description = "Partition Is Armed Away"
                    	break
                	case "1":
                    	value = "armedStay"
			            description = "Partition Is Armed Stay"
                    	break
                	case "2":
                    	value = "armedZeroEntryAway"
			            description = "Partition Is Armed Zero Entry Away"
                    	break
                	case "3":
                    	value = "armedZeroEntryStay"
			            description = "Partition Is Armed Zero Entry Stay"
                    	break
                    default:
                        value = "armed"
			            description = "Partition Is Armed"
                }
            } else {
            	value = "armed"
			    description = "Partition Is Armed"
            }
        	break
        case "653":
        	value = "readyForceArming"
			description = "Partition Is Ready For Force Arming"
            break
        case "654":
        	value = "alarm"
			description = "Partition Is In Alarm"
            break
        case "655":
        	value = "disarmed"
			description = "Partition Is Disarmed"
            break
        case "656":
        	value = "exitDelay"
			description = "Partition Is In Exit Delay"
            break
        case "657":
        	value = "entryDelay"
			description = "Partition Is In Entry Delay"
            break
        case "658":
        	value = "keypadLockout"
			description = "Partition Is In Keypad Lockout"
            break
    	default:
        	value = "unknown"
			description = "Partition Status Is Unknown"
	}
    log.debug "Sending event [event=partition, value=$value, descriptionText=$description]"
    sendEvent(name: "partition", value: value, descriptionText: description)
}

// handle commands
def refresh() {
	log.debug "Executing 'refresh'"
    parent.pollEnvisalink()
}

def arm() {
    def part = device.deviceNetworkId
	log.debug "Executing 'arm' for partition $part"
	parent.arm(part)
}

def armStay() {
    def part = device.deviceNetworkId
	log.debug "Executing 'armStay' for partition $part"
	parent.armStay(part)
}

def armAway() {
    def part = device.deviceNetworkId
	log.debug "Executing 'armAway' for partition $part"
	parent.armAway(part)
}

def armZeroEntryDelay() {
    def part = device.deviceNetworkId
	log.debug "Executing 'armZeroEntryDelay' for partition $part"
	parent.armZeroEntryDelay(part)    
}

def disarm() {
    def part = device.deviceNetworkId
	log.debug "Executing 'disarm' for partition $part"
	parent.disarm(part)
}


