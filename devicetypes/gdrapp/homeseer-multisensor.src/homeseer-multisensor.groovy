metadata {
	// Automatically generated. Make future change here.
	definition (name: "HomeSeer Multisensor", namespace: "gdrapp", author: "Greg Rapp") {
		capability "Motion Sensor"
		capability "Temperature Measurement"
		capability "Configuration"
		capability "Illuminance Measurement"
		capability "Sensor"
		capability "Battery"

		fingerprint deviceId: "0x2101", inClusters: "0x60,0x31,0x70,0x84,0x85,0x80,0x72,0x77,0x86"
	}

	simulator {
		// messages the device returns in response to commands it receives
		status "motion (basic)"     : "command: 2001, payload: FF"
		status "no motion (basic)"  : "command: 2001, payload: 00"
		status "76.7 deg F"         : "command: 6006, payload: 03 31 05 01 2A 02 FF"
		status "80.6 deg F"         : "command: 6006, payload: 03 31 05 01 2A 03 26"
		status "2  lux"             : "command: 6006, payload: 02 31 05 03 01 02"
		status "80 lux"             : "command: 6006, payload: 02 31 05 03 01 50"
		for (int i = 0; i <= 100; i += 20) {
			status "battery ${i}%": new physicalgraph.zwave.Zwave().batteryV1.batteryReport(
				batteryLevel: i).incomingMessage()
		}
	}

	tiles {
		standardTile("motion", "device.motion", width: 2, height: 2) {
			state "active", label:'motion', icon:"st.motion.motion.active", backgroundColor:"#53a7c0"
			state "inactive", label:'no motion', icon:"st.motion.motion.inactive", backgroundColor:"#ffffff"
		}
		valueTile("temperature", "device.temperature", inactiveLabel: false) {
			state "temperature", label:'${currentValue}°',
			backgroundColors:[
				[value: 31, color: "#153591"],
				[value: 44, color: "#1e9cbb"],
				[value: 59, color: "#90d2a7"],
				[value: 74, color: "#44b621"],
				[value: 84, color: "#f1d801"],
				[value: 95, color: "#d04e00"],
				[value: 96, color: "#bc2323"]
			]
		}
		valueTile("illuminance", "device.illuminance", inactiveLabel: false) {
			state "luminosity", label:'${currentValue} ${unit}', unit:"lux"
		}
		valueTile("battery", "device.battery", inactiveLabel: false, decoration: "flat") {
			state "battery", label:'${currentValue}% battery', unit:""
		}
		standardTile("configure", "device.configure", inactiveLabel: false, decoration: "flat") {
			state "configure", label:'', action:"configuration.configure", icon:"st.secondary.configure"
		}

		main(["motion", "temperature", "illuminance"])
		details(["motion", "temperature", "illuminance", "battery"])
	}

	preferences {
		input "intervalMins", "number", title: "Multisensor report  (minutes)", description: "Minutes between temperature/illuminance readings", defaultValue: "${intervalMins}", required: true, displayDuringSetup: true
		input "motionOnTimeMins", "number", title: "Motion Timeout  (0-127 minutes)", description: "Minutes until device sends an OFF command", defaultValue: "${motionOnTimeMins}", required: true, displayDuringSetup: true
		//input "motionSensitivity", "number", title: "Motion Sensitivity (0-255)", description: "Motion required to detect motion (higher is more sensitive)", defaultValue: "${motionSensitivity}", required: false, displayDuringSetup: true
		input "motionSensitivity", "enum", title: "Motion Sensitivity", defaultValue: "${motionSensitivity}", required: true, displayDuringSetup: true, options:[10:"Low", 100:"Medium", 200:"High"]
		input "ledOnOff", "enum", title: "LED Enabled", description: "LED enabled when motion is detected", defaultValue: ledOnOff, displayDuringSetup: true, options:[true:"Yes", false:"No"]
	}
}

// Parse incoming device messages to generate events
def parse(String description) {
	def result = null
	def cmd = zwave.parse(description, [0x31: 1, 0x84: 2, 0x60: 1, 0x85: 1, 0x70: 1])

	if (cmd) {
		result = zwaveEvent(cmd)
	}
	// log.debug "Parsed ${description.inspect()} to ${result.inspect()}"
	return result
}

def zwaveEvent(physicalgraph.zwave.commands.multiinstancev1.MultiInstanceCmdEncap cmd) {
	def encapsulated = null
	log.debug "***Pre-Encapsulated as: $cmd"
	if (cmd.respondsTo("encapsulatedCommand")) {

		encapsulated = cmd.encapsulatedCommand()
		log.debug "***Encapsulated as: $encapsulated"
	} else {

		def hex1 = { n -> String.format("%02X", n) }
		def sorry = "command: ${hex1(cmd.commandClass)}${hex1(cmd.command)}, payload: " + cmd.parameter.collect{ hex1(it) }.join(" ")
		encapsulated = zwave.parse(sorry, [0x31: 1, 0x84: 2, 0x60: 1, 0x85: 1, 0x70: 1])
	}

	return encapsulated ? zwaveEvent(encapsulated) : null
}

def zwaveEvent(physicalgraph.zwave.commands.wakeupv2.WakeUpNotification cmd) {
	def results = [createEvent(descriptionText: "${device.displayName} woke up", isStateChange: false)]
	
	if (state.config) {
		state.config = false
		results << response(configure())
	}
	def prevBattery = device.currentState("battery")
	if (!prevBattery || (new Date().time - prevBattery.date.time)/60000 >= 60 * 53) {
		results << response(zwave.batteryV1.batteryGet().format())
        results << response(zwave.configurationV1.configurationGet(parameterNumber: 1).format())
        results << response(zwave.configurationV1.configurationGet(parameterNumber: 2).format())
        results << response(zwave.configurationV1.configurationGet(parameterNumber: 3).format())
	}
	results << response(temperatureGetCmd().format())
	results << response(illuminanceGetCmd().format())

	results << response(zwave.wakeUpV1.wakeUpNoMoreInformation().format())
	return results
}

def zwaveEvent(physicalgraph.zwave.commands.sensormultilevelv5.SensorMultilevelReport cmd)
{
	def map = [:]
	switch (cmd.sensorType) {
		case 1:
			// temperature
			def cmdScale = cmd.scale == 1 ? "F" : "C"
			map.value = convertTemperatureIfNeeded(cmd.scaledSensorValue, cmdScale, cmd.precision)
			map.unit = getTemperatureScale()
			map.name = "temperature"
			break;
		case 3:
			// luminance
			map.value = cmd.scaledSensorValue.toInteger().toString()
			map.unit = "lux"
			map.name = "illuminance"
			break;
		case 5:
			// humidity
			map.value = cmd.scaledSensorValue.toInteger().toString()
			map.unit = "%"
			map.name = "humidity"
			break;
	}
	createEvent(map)
}

def zwaveEvent(physicalgraph.zwave.commands.configurationv1.ConfigurationReport cmd)
{
	switch (cmd.parameterNumber) {
		case 1:
			// motion sensitivity
            log.debug "Motion sensitivity configuration reported by device as ${cmd.configurationValue[0]}"
            settings.motionSensitivity = cmd.configurationValue[0]
			break;
		case 2:
			// motion on-time
            log.debug "Motion on time minutes configuration reported by device as ${cmd.configurationValue[0]}"
            settings.motionOnTimeMins = cmd.configurationValue[0]
			break;
		case 3:
			// LED on/off
            log.debug "LED on/off configuration reported by device as ${(cmd.configurationValue[0]==0 ? 'off' : 'on')}"
            settings.ledOnOff = (cmd.configurationValue[0]==0 ? false : true)
			break;
	}
}

def zwaveEvent(physicalgraph.zwave.commands.batteryv1.BatteryReport cmd) {
	def map = [ name: "battery", unit: "%" ]
	if (cmd.batteryLevel == 0xFF) {
		map.value = 1
		map.descriptionText = "$device.displayName has a low battery!"
	} else {
		map.value = cmd.batteryLevel
	}
	createEvent(map)
}

def zwaveEvent(physicalgraph.zwave.commands.sensorbinaryv1.SensorBinaryReport cmd) {
	def map = [:]
	map.value = cmd.sensorValue ? "active" : "inactive"
	map.name = "motion"
	if (map.value == "active") {
		map.descriptionText = "$device.displayName detected motion"
	}
	else {
		map.descriptionText = "$device.displayName motion has stopped"
	}
	createEvent(map)
}

def zwaveEvent(physicalgraph.zwave.commands.basicv1.BasicSet cmd) {
	def map = [:]
	map.value = cmd.value ? "active" : "inactive"
	map.name = "motion"
	if (map.value == "active") {
		map.descriptionText = "$device.displayName detected motion"
	}
	else {
		map.descriptionText = "$device.displayName motion has stopped"
	}
	createEvent(map)
}

def zwaveEvent(physicalgraph.zwave.Command cmd) {
	createEvent(displayed: false, descriptionText: "$device.displayName: $cmd")
}

def temperatureGetCmd() {
	zwave.multiInstanceV1.multiInstanceCmdEncap(instance:3, commandClass:0x31, command:0x04)
}

def illuminanceGetCmd() {
	zwave.multiInstanceV1.multiInstanceCmdEncap(instance:2, commandClass:0x31, command:0x04)
}

def updated() {
	log.debug "Updated with settings: ${settings}"
	state.config = true
}

def configure() {
	def cmds = []
    if (intervalMins) {
        log.debug "Will configure wakeup interval (${60 * (intervalMins ?: 20).toInteger()} seconds)"
        cmds << zwave.wakeUpV2.wakeUpIntervalSet(seconds: 60 * (intervalMins ?: 20).toInteger(), nodeid: zwaveHubNodeId).format()    
    }
    
    if (motionSensitivity) {
        log.debug "Will configure motion sensitivity (${(motionSensitivity ?: 200).toInteger()})"
        cmds << zwave.configurationV1.configurationSet(parameterNumber: 1, configurationValue: [(motionSensitivity ?: 200).toInteger()], size: 1).format()    
    }
    
    if (motionOnTimeMins) {
        log.debug "Will configure motion on time (${(motionOnTimeMins ?: 20).toInteger()} minutes)"
        cmds << zwave.configurationV1.configurationSet(parameterNumber: 2, configurationValue: [(motionOnTimeMins ?: 20).toInteger()], size: 1).format()    
    }
    
	log.debug "Will configure LED on/off value (${(ledOnOff ? 'on' : 'off')})"
    cmds << zwave.configurationV1.configurationSet(parameterNumber: 3, configurationValue: [(ledOnOff ? -1 : 0)], size: 1).format()
}