import socket    
import requests
import time

hostname = socket.gethostname()    
my_ip = socket.gethostbyname(hostname)    
# print("Your Computer Name is:" + hostname)    
# print("Your Computer IP Address is:" + my_ip) 

url = 'https://scan-app2020.herokuapp.com/scanapp/connect'
param = {'my_ip':my_ip}

while True:
    response = requests.get(url = url, params = param)
    data = response.json()

    if response.status_code == 500:
        print(data['message'])
        time.sleep(30)

    else:
        print(data)
        print(data['hotspot_name'])
        print(data['pass'])
        break
