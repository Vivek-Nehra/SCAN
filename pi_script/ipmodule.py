import socket    
import requests
import time
from wifi_connect import connect_to_wifi

hostname = socket.gethostname()    
my_ip = socket.gethostbyname(hostname)    
# print("Your Computer Name is:" + hostname)    
# print("Your Computer IP Address is:" + my_ip) 

url = 'https://scan-app2020.herokuapp.com/scanapp/connect'
param = {'my_ip': '192.168.43.21'}

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
print(ssid,psk)
establish_connection = connect_to_wifi(ssid,psk)
if establish_connection == True:
    print ("Connected to WIFI !")
    # Logic to create Socket connection
else:
    print ("Error Found")
    # Logic to Send Request to server of failed connection
