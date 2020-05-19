import re
import subprocess
import time


def connect_to_wifi(name,pswd):
	global log
	log = open("logger.txt", "w")
	try:
		add_wifi(name,pswd)
		subprocess.call("sudo wpa_cli -i wlan0 reconfigure".split())
		start_time, wait_time = [time.time()]*2
		log.write("Refreshed Wifi")
		while(not subprocess.check_output("hostname -I".split()).strip()):
			wait_time = time.time()-start_time
			if wait_time >= 20:
				print("Connection taking too long. Try again later")
				log.write("Connection taking too long. Try again later.\n")
				delete_wifi(name,pswd)
				return False
			continue

		connected_device = subprocess.check_output(['sudo','iwgetid'])
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
		print(text)
		if delete_text in text:
			text = re.sub(delete_text, "", text)
			fp.seek(0)
			fp.write(text)
			fp.truncate()

