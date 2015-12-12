/**
 *  Envisalink Thing
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
    name: "Envisalink Thing",
    namespace: "gdrapp",
    author: "Greg Rapp",
    description: "Envisalink",
    category: "Safety & Security",
    iconUrl: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience.png",
    iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png",
    iconX3Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png",
    oauth: true,
    singleInstance: true)

mappings {
  path("/update") {
    action: [
      POST: "postUpdate"
    ]
  }
}

preferences {
	page(name:"config", title:"Envisalink Thing Server", nextPage:"envisalinkDiscovery", uninstall:true) {
    	section("Address") {
			input("envisalinkIP", "text", title:"IP Address", required:false)
			input("envisalinkPort", "number", title:"Port Number", required:false)
        }
    	section("Authentication") {
			input("envisalinkUsername", "text", title:"Username", capitalization:"none", required:false)
			input("envisalinkPassword", "password", title:"Password", required:false)
        }
    }
	page(name:"envisalinkDiscovery")
    page(name:"selectCapabilities")
    page(name:"modePrefs", title:"Mode Preferences", nextPage:"notificationPrefs") {
        section() {
            paragraph "Hello, Home Mode Preferences"
            input("helloHomePartitions", "enum", required:false, title:"Select Partitions", multiple:true, options:selectedPartitions)
        }
        section("Disarm alarm when mode changes to:") {
            input("helloDisarm", "mode", title: "Disarm", required:false)
        }
        section("Arm alarm away when mode changes to:") {
            input("helloArmAway", "mode", title: "Arm", required:false)
        }
        section("Arm alarm with zero delay when mode changes to:") {
            input("helloArmZeroDelay", "mode", title: "Arm No Delay", required:false)
        }
    }
    page(name:"notificationPrefs", title:"Notification Preferences", nextPage:"", uninstall:true, install:true) {
        section("Arm Notifications") {
            input("armRecipients", "contact", title: "Send notifications to") {
                input "armPhone", "phone", title: "Notify with text message (optional)",
                    description: "Phone Number", required: false
            }
        }
        section("Disarm Notifications") {
            input("disarmRecipients", "contact", title: "Send notifications to") {
                input "disarmPhone", "phone", title: "Notify with text message (optional)",
                    description: "Phone Number", required: false
            }
        }
        section("Alarm Notifications") {
            input("alarmRecipients", "contact", title: "Send notifications to") {
                input "alarmPhone", "phone", title: "Notify with text message (optional)",
                    description: "Phone Number", required: false
            }
        }
    }
}
//PAGES
def envisalinkDiscovery() {
    int discoveryRefreshCount = !state.discoveryRefreshCount ? 0 : state.discoveryRefreshCount as int
	state.discoveryRefreshCount = discoveryRefreshCount + 1

	def refreshInterval = 10

    def partitionOptions = getPartitionDevices()
    def zoneOptions = getZoneDevices()

    partitionOptions = partitionOptions.sort { it.value }
    zoneOptions = zoneOptions.sort { it.value }

	def numPartitionsFound = partitionOptions.size() ?: 0
	def numZonesFound = zoneOptions.size() ?: 0

    // Only request a status update from Envisalink every 3 refreshes
    if (discoveryRefreshCount % 3 == 0) {
      pollEnvisalink()
    }

    dynamicPage(name:"envisalinkDiscovery", title:"Discovery Started!", nextPage:"selectCapabilities", refreshInterval:refreshInterval, uninstall: true) {
        section("Please wait while we discover your security system. Discovery should take about one minute. Select your active partitions and zones below once discovered.") {
            input("selectedPartitions", "enum", required:false, title:"Select Partitions ($numPartitionsFound found)", multiple:true, options:partitionOptions)
            input("selectedZones", "enum", required:false, title:"Select Zones ($numZonesFound found)", multiple:true, options:zoneOptions)
        }
    }
}

def selectCapabilities() {
	def optionsContact = getZoneDevices().subMap(selectedZones?:[])
	def optionsMotion = getZoneDevices().subMap(selectedZones?:[])
	def optionsSmoke = getZoneDevices().subMap(selectedZones?:[])
	def optionsCO = getZoneDevices().subMap(selectedZones?:[])

	optionsContact.keySet().removeAll((motionZones?:[])+(smokeZones?:[])+(coZones?:[]))
    optionsMotion.keySet().removeAll((contactZones?:[])+(smokeZones?:[])+(coZones?:[]))
    optionsSmoke.keySet().removeAll((contactZones?:[])+(motionZones?:[])+(coZones?:[]))
    optionsCO.keySet().removeAll((contactZones?:[])+(motionZones?:[])+(smokeZones?:[]))

	dynamicPage(name:"selectCapabilities", title:"Define Zone Types", nextPage:"modePrefs", uninstall:true) {
		section {
        	input("contactZones", "enum", required:false, title: "Open/Close Zones", multiple:true, options:optionsContact, submitOnChange:true)
        	input("motionZones", "enum", required:false, title: "Motion Zones", multiple:true, options:optionsMotion, submitOnChange:true)
        	input("smokeZones", "enum", required:false, title: "Smoke Zones", multiple:true, options:optionsSmoke, submitOnChange:true)
        	input("coZones", "enum", required:false, title: "Carbon Monoxide Zones", multiple:true, options:optionsCO, submitOnChange:true)
        }
	}
}

def envisalinkCommand(path) {
	if (envisalinkIP && envisalinkPort && envisalinkUsername && envisalinkPassword) {
        log.debug "Requesting status from Envisalink at $envisalinkIP:$envisalinkPort"
        def authEncoded = "$envisalinkUsername:$envisalinkPassword".bytes.encodeBase64()
        def ha = new physicalgraph.device.HubAction(
            method: "GET",
            path: path,
            headers: [
                HOST: "$envisalinkIP:$envisalinkPort",
                Authorization: "Basic $authEncoded"
            ]
        )
        sendHubCommand(ha)
    } else {
    	if (!envisalinkIP) {
	        log.debug "Unable to request $path from Envisalink since IP address has not been configured in the SmartApp"
        }
    	if (!envisalinkPort) {
	        log.debug "Unable to request $path from Envisalink since communication port has not been configured in the SmartApp"
        }
    	if (!envisalinkUsername) {
	        log.debug "Unable to request $path from Envisalink since username has not been configured in the SmartApp"
        }
    	if (!envisalinkPassword) {
	        log.debug "Unable to request $path from Envisalink since password has not been configured in the SmartApp"
        }
    }
}

def getPartitionDevices() {
	state.partitionDevices = ((state.partitionDevices ?: [:]) as LinkedHashMap).sort { it.value }
}

def getZoneDevices() {
	state.zoneDevices = ((state.zoneDevices ?: [:]) as LinkedHashMap).sort { it.value }
}

def installed() {
	log.debug "Installed SmartApp with settings: ${settings}"
	initialize()
    subscribe()
}

def updated() {
	log.debug "Updated SmartApp with settings: ${settings}"
    unsubscribe()
	initialize()
    subscribe()
}

def uninstalled() {
	def devices = getChildDevices()
	log.debug "Deleting ${devices.size()} Envisalink child devices since the SmartApp is being uninstalled"
	devices.each {
    	log.trace("Deleting child device ${it.deviceNetworkId} because we're uninstalling the SmartApp")
		deleteChildDevice(it.deviceNetworkId)
	}
}

def subscribe() {
	subscribe(location, "mode", modeChangeHandler)
    
    def partitionDevices = getChildDevices().findAll { it.deviceNetworkId.startsWith("PARTITION") }
    subscribe(partitionDevices, "", partitionEventHandler)
}

def initialize() {
    def delete = getChildDevices().findAll {
    	!selectedZones?.contains(it.deviceNetworkId) &&
        !selectedPartitions?.contains(it.deviceNetworkId) &&
        it.deviceNetworkId!="PANEL"
    }
    delete.each {
    	log.debug "Deleting device ${it.deviceNetworkId} because it is no longer selected"
        deleteChildDevice(it.deviceNetworkId)
        contactZones?.remove(it.deviceNetworkId)
        motionZones?.remove(it.deviceNetworkId)
        smokeZones?.remove(it.deviceNetworkId)
        coZones?.remove(it.deviceNetworkId)
    }

    addPanel()

    if (selectedPartitions) {
        addPartitions()
	}

    if (contactZones || motionZones || smokeZones || coZones) {
		addZones()
	}

	if (selectedPartitions || contactZones || motionZones || smokeZones || coZones) {
	    pollEnvisalink()    
    }
}

def addPanel() {
	def dni = "PANEL"
	def d = getChildDevice(dni)
	if(!d) {
		d = addChildDevice("gdrapp", "Envisalink Panel", dni, null, [label:"Envisalink Panel"])
		log.debug "Created child device ${d.displayName} with ID $dni"
	} else {
		log.debug "Child device ${d.displayName} with ID $dni already exists"
	}
}

def addPartitions() {
	def partitionDevices = getPartitionDevices()
	selectedPartitions.each { dni ->
		def d = getChildDevice(dni)
		if(!d) {
			def newDevice = partitionDevices.find { it.key == dni }
			d = addChildDevice("gdrapp", "Envisalink Partition", dni, null, [label:"${newDevice?.value}"])
			log.debug "Created child device ${d.displayName} with ID $dni"
		} else {
			log.debug "Child device ${d.displayName} with ID $dni already exists"
		}
	}
}

def addZones() {
	def devices = getZoneDevices()
	contactZones.each { dni ->
		def d = getChildDevice(dni)
		if(!d) {
			def newDevice = devices.find { it.key == dni }
			d = addChildDevice("gdrapp", "Envisalink Contact Zone", dni, null, [label:"${newDevice?.value}"])
			log.debug "Created child device contact zone ${d.displayName} with ID $dni"
		} else {
			log.debug "Child device contact zone ${d.displayName} with ID $dni already exists"
		}
	}

	motionZones.each { dni ->
		def d = getChildDevice(dni)
		if(!d) {
			def newDevice = devices.find { it.key == dni }
			d = addChildDevice("gdrapp", "Envisalink Motion Zone", dni, null, [label:"${newDevice?.value}"])
			log.debug "Created child device motion zone ${d.displayName} with ID $dni"
		} else {
			log.debug "Child device motion zone ${d.displayName} with ID $dni already exists"
		}
	}

	smokeZones.each { dni ->
		def d = getChildDevice(dni)
		if(!d) {
			def newDevice = devices.find { it.key == dni }
			d = addChildDevice("gdrapp", "Envisalink Smoke Zone", dni, null, [label:"${newDevice?.value}"])
			log.debug "Created child device smoke zone ${d.displayName} with ID $dni"
		} else {
			log.debug "Child device smoke zone ${d.displayName} with ID $dni already exists"
		}
	}

	coZones.each { dni ->
		def d = getChildDevice(dni)
		if(!d) {
			def newDevice = devices.find { it.key == dni }
			d = addChildDevice("gdrapp", "Envisalink Carbon Monoxide Zone", dni, null, [label:"${newDevice?.value}"])
			log.debug "Created child device CO zone ${d.displayName} with ID $dni"
		} else {
			log.debug "Child device CO zone ${d.displayName} with ID $dni already exists"
		}
	}
}

def postUpdate() {
	log.trace "Update posted to SmartApp from Envisalink: ${request?.JSON}"
	def cmd = request?.JSON?.cmd
	def payload = request?.JSON?.payload

    def zoneCmds = 601..610
    def partitionCmds = 650..674
    def panelCmds = [800,801,802,803,806,807,814,816,829,830]

        // Zone
        if (zoneCmds.contains(cmd?.toInteger())) {
            log.debug "Received zone update"
            def partition,zone
            if (payload != null && payload.length() == 4) {
                partition = payload[0]
                zone = payload.substring(1)
            } else {
                zone = payload
            }
            def zoneDevices = getZoneDevices()
            if (!(zoneDevices."ZONE${zone}")) {
                log.trace "Adding new zone device to SmartApp device state"
                zoneDevices << ["ZONE${zone}":"Envisalink Zone ${zone}"]
            }
            def device = getChildDevice("ZONE${zone}")
            if (device) {
                device.handleUpdate(cmd, payload)
            }
        }
        // Partition
        else if (partitionCmds.contains(cmd?.toInteger())) {
            log.debug "Received partition update"
            if (payload != null && payload.length() >= 1) {
                def partition = payload[0]
                def partitionDevices = getPartitionDevices()
                if (!(partitionDevices."PARTITION${partition}")) {
                    log.trace("Adding new partition device to SmartApp device state")
                    partitionDevices << ["PARTITION${partition}":"Envisalink Partition ${partition}"]
                }
                def device = getChildDevice("PARTITION${partition}")
                if (device) {
                    device.handleUpdate(cmd, payload)
                }
            }
        }
        // Panel
        else if (panelCmds.contains(cmd?.toInteger())) {
            log.debug "Received panel update"
            def device = getChildDevice("PANEL")
            if (device) {
                device.handleUpdate(cmd, payload)
            }
        }
}

private getPartitionNumber(dni) {
	if (dni.startsWith("PARTITION")) {
        dni -= "PARTITION"
        if (dni.isInteger()) {
            return dni
        } else {
            return null
        }    
    } else {
    	return null
    }
}

def modeChangeHandler(evt) {
    log.debug "Mode event ${evt.name} fired"

    // get the value of this event, e.g., "on" or "off"
    log.debug "The value of this event is ${evt.value}"

    // get the Date this event happened at
    log.debug "This event happened at ${evt.date}"

    // did the value of this event change from its previous state?
    log.debug "The value of this event is different from its previous value: ${evt.isStateChange()}"
    if (evt.value == helloDisarm && evt.isStateChange) {
    	/*helloHomePartitions?.each {
    		disarm(it)        
        }*/
        disarm("PARTITION1")
    }
    if (evt.value == helloArmAway && evt.isStateChange) {
        /*helloHomePartitions?.each {
        	armAway(it)
        }*/
        armAway("PARTITION1")
    }
    if (evt.value == helloArmZeroDelay && evt.isStateChange) {
        /*helloHomePartitions?.each {
        	armZeroEntryDelay(it)
        }*/
        armZeroEntryDelay("PARTITION1")
    }
}

def pollEnvisalink() {
	log.debug "Sending poll request to Envisalink Thing server"
	envisalinkCommand("/status")
}

def arm(partition) {
	partition = getPartitionNumber(partition)
	if (partition) {
		log.debug "Sending Envisalink the arm command for partition $partition"
		envisalinkCommand("/action/arm/$partition")
    } else {
    	log.error "Unable to execute arm because partition is invalid [$partition]"
    }
}

def armAway(partition) {
	partition = getPartitionNumber(partition)
	if (partition) {
		log.debug "Sending Envisalink the armAway command for partition $partition"
		envisalinkCommand("/action/armAway/$partition")
    } else {
    	log.error "Unable to execute armAway because partition is invalid [$partition]"
    }
}

def armStay(partition) {
	partition = getPartitionNumber(partition)
	if (partition) {
		log.debug "Sending Envisalink the armStay command for partition $partition"
		envisalinkCommand("/action/armStay/$partition")
    } else {
    	log.error "Unable to execute armStay because partition is invalid [$partition]"
    }
}

def armZeroEntryDelay(partition) {
	partition = getPartitionNumber(partition)
	if (partition) {
		log.debug "Sending Envisalink the armZeroEntryDelay command for partition $partition"
		envisalinkCommand("/action/armZeroEntryDelay/$partition")
    } else {
    	log.error "Unable to execute armZeroEntryDelay because partition is invalid [$partition]"
    }
}

def disarm(partition) {
	partition = getPartitionNumber(partition)
	if (partition) {
		log.debug "Sending Envisalink the disarm command for partition $partition"
		envisalinkCommand("/action/disarm/$partition")
    } else {
    	log.error "Unable to execute disarm because partition is invalid [$partition]"
    }
}

def partitionEventHandler(evt) {
    // get the event name, e.g., "switch"
    log.debug "This event name is ${evt.name}"

    // get the value of this event, e.g., "on" or "off"
    log.debug "The value of this event is ${evt.value}"

    // get the Date this event happened at
    log.debug "This event happened at ${evt.date}"

    // did the value of this event change from its previous state?
    log.debug "The value of this event is different from its previous value: ${evt.isStateChange()}"
    
    if (evt.name == "partition") {
        def message = evt.descriptionText
    	if (["armed","armedAway","armedStay","armedZeroEntryAway","armedZeroEntryStay"].contains(evt.value)) {
            if (location.contactBookEnabled && armRecipients) {
                log.debug "contact book enabled!"
                sendNotificationToContacts(message, armRecipients)
            } else {
                log.debug "contact book not enabled"
                if (armPhone) {
                    sendSms(armPhone, message)
                }
            }            
        } else if ("disarmed" == evt.value) {
            if (location.contactBookEnabled && disarmRecipients) {
                log.debug "contact book enabled!"
                sendNotificationToContacts(message, disarmRecipients)
            } else {
                log.debug "contact book not enabled"
                if (disarmPhone) {
                    sendSms(disarmPhone, message)
                }
            }            
        } else if ("alarm" == evt.value) {
            if (location.contactBookEnabled && alarmRecipients) {
                log.debug "contact book enabled!"
                sendNotificationToContacts(message, alarmRecipients)
            } else {
                log.debug "contact book not enabled"
                if (alarmPhone) {
                    sendSms(alarmPhone, message)
                }
            }            
        } 
    }
}
