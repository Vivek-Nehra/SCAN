import socket    
import requests
import time
from wifi_connect import connect_to_wifi

hostname = socket.gethostname()    
my_ip = socket.gethostbyname(hostname)
# print("Your Computer Name is:" + hostname)    
# print("Your Computer IP Address is:" + my_ip)

url = 'https://scan-app2020.herokuapp.com/scanapp/connect'
param = {'bikeIP': '192.168.43.21'}

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
android_ip = str(data['androidIP'])
print(ssid, psk, android_ip)

establish_connection = connect_to_wifi(ssid, psk)
if establish_connection == True:
    print ("Connected to WIFI !")
    # Logic to create Socket connection
else:
    print ("Error Found")
    url = 'https://scan-app2020.herokuapp.com/scanapp/bike_status'
    param = {'androidIP': android_ip, 'status': 'can't connect}
    response = requests.get(url = url, params = param)

    # Logic to Send Request to server of failed connection
