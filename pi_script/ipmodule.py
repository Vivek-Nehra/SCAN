import requests
import time
import wifi_connect
import socketmodule


def connect_to_server():
    print(wifi_connect.get_ip_address())
    while True:
        url = 'https://scan-app2020.herokuapp.com/scanapp/connect'
        param = {'bikeIP': wifi_connect.get_ip_address()}

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
    android_ip = str(data['androidIP'])
    global android_ip
    print(ssid, psk, android_ip)

    establish_connection = wifi_connect.connect_to_wifi(ssid,psk)
    if establish_connection == True:
        print ("Connected to WIFI !")
        socketmodule.startClient(android_ip, b"Connection Established\n")
        socketmodule.startServer(wifi_connect.get_ip_address())

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
