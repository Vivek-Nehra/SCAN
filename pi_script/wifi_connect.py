import re
import subprocess
import time

def get_ip_address():
	return(str(subprocess.check_output("hostname -I".split())).split()[0])

def refresh_wifi():
	subprocess.call("sudo wpa_cli -i wlan0 reconfigure".split())

def flush_wifi():
	subprocess.call("sudo cp /etc/wpa_supplicant/temp_wpa_supplicant.conf /etc/wpa_supplicant/wpa_supplicant.conf".split())

def check_available_wifi():
	devices = subprocess.check_output("sudo iwlist wlan0 scan".split())
	subs = ""
	for line in devices.splitlines():
		if "ESSID" in line:
			subs += line.split('"')[1] + '\n'
	return subs

def check_routers():
	try:
		with open("/etc/wpa_supplicant/temp_wpa_supplicant.conf","r+") as fp:
			text = "".join(fp.readlines())
			routers = ""
			for line in text.splitlines():
				if "ssid" in line:
					routers += line.split('"')[1] + '\n'
			return routers

	except Exception as e:
		print(str(e))

def find_available_routers():
	log = open("logger.txt", "w")
	log.write("Opening Log")
	devices = check_available_wifi()
	log.write("Devices: " + devices + "\n")
	routers = check_routers()
	log.write("Routers : " + routers + "\n")
	match = "".join([x for x in routers.splitlines() if x in devices.splitlines()])
	log.write("Match : " + match + "\n")
	return (match)

def connect_to_wifi(name,pswd):
	global log
	log = open("logger.txt", "w")
	refresh_wifi()
	try:
		add_wifi(name,pswd)
		start_time, wait_time = [time.time()]*2
		refresh_wifi()
		log.write("Refreshed wifi \n")
		while(len(str(subprocess.check_output("sudo iwgetid wlan0 -r".split())).strip()) == 0):
			wait_time = time.time()-start_time
			if wait_time >= 20:
				print("Connection taking too long. Try again later")
				log.write("Connection taking too long. Try again later.\n")
				delete_wifi(name,pswd)
				return False
			continue

		connected_device = subprocess.check_output("sudo iwgetid wlan0 -r".split())
		print(connected_device)
		log.write("Connected Device: " + str(connected_device))

		if (name not in connected_device):
			delete_wifi(name,pswd)
			log.write("Connection with required device not established. Try again later.")
			return False
		else:
			log.write("Connected to wifi device")
			return True

	except Exception as e:
		print(e)
		delete_wifi(name,pswd)
		log.write("Error: " + str(e) + "\n")
		return False
	finally:
		log.close()

def add_wifi(name,pswd):
	append_text = '''network={
	ssid="''' + name + '''"
	psk="''' + pswd + '''"
	priority=5
}
'''

	try:
		with open("/etc/wpa_supplicant/wpa_supplicant.conf","r+") as fp:
			text = "".join(fp.readlines())
			if append_text not in text:
				fp.write(append_text)
			log.write("Wifi added successfully\n")
	except Exception as e:
		raise e

def delete_wifi(name,pswd):
	delete_text = '''network={
	ssid="''' + name + '''"
	psk="''' + pswd + '''"
	priority=5
}
'''

	with open("/etc/wpa_supplicant/wpa_supplicant.conf","r+") as fp:
		text = "".join(fp.readlines())
		if delete_text in text:
			text = re.sub(delete_text, "", text)
			fp.seek(0)
			fp.write(text)
			fp.truncate()

