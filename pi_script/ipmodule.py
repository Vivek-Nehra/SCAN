import requests
import time
import wifi_connect
import socketmodule
import hardware_control


def connect_to_server():
    print(wifi_connect.get_ip_address())
    url = 'https://scan-app2020.herokuapp.com/scanapp/connect'
    param = {'bikeIP': wifi_connect.get_ip_address()}

    while True:
        response = requests.get(url = url, params = param)
        data = response.json()

        if response.status_code == 500:
            print(data['message'])
            time.sleep(3)

        else:
            print(data)
            break

    ssid = str(data['hotspot_name'])
    psk = str(data['pass'])
    global android_ip
    android_ip = str(data['androidIP'])
    print(ssid, psk, android_ip)

    establish_connection = wifi_connect.connect_to_wifi(ssid,psk)
    if establish_connection == True:
        print ("Connected to WIFI !")
        # Logic to create Socket connection
        reply = socketmodule.send_message(android_ip,b"Connection Established \n")
	print(reply)
	if reply:
		hardware_control.connected()
		released = socketmodule.start_server_socket()
		if released:
			print("Restarting server again")
			wifi_connect.delete_wifi(ssid,psk)
			wifi_connect.refresh_wifi()
			connect_to_server()
	else:
		connect_to_server()

    else:
        print ("Error Found")
        send_error_message(20)
        connect_to_server()

def send_error_message(timeout):
        try:
		if timeout == 0:
			return
		url = 'https://scan-app2020.herokuapp.com/scanapp/bike_status'
		param = {'androidIP': android_ip, 'status': "can't connect"}
		response = requests.get(url = url, params = param)
		print(response)
		return
        except Exception as e:
		print(str(e))
		time.sleep(1)
		timeout -= 1
		send_error_message(timeout)


if __name__ == "__main__":
    connect_to_server()
