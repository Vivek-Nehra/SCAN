#!/usr/bin/env python3
 
import socket
from wifi_connect import get_ip_address
import _thread
import time
import hardware_control


def start_server_socket():

    HOST = str(get_ip_address())
    PORT = 9008


    server = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
    server.bind((HOST, PORT))
    server.listen(5)
    print("Starting Server to receive commands")
    while True:
	try:
		global conn
	    	conn, addr = server.accept()

    		msg_from_app = ''
    		#while True:
	        # Receiving data in form of chunks of size 4096 (can be changed as per usage).
	        msg_chunk = conn.recv(4096)
	        # Data is receieved in the form of bytes decode it into String type.
	        msg_chunk = msg_chunk.decode()

	        #if not msg_chunk: 
	        	#break
	        msg_from_app += msg_chunk
	        print ("Data : " + msg_from_app)

		if "Lock" in msg_from_app:
			print("Locking")
			conn.sendall(b"Locked \n")
			hardware_control.lock()

		elif "Unlock" in msg_from_app:
			print("Unlocking")
			conn.send(b"Unlocked \n")
			hardware_control.unlock()

		elif "Release" in msg_from_app:
			print("Releasing")
			conn.send(b"Released \n")
			hardware_control.release()
			return True
		else:
			print("Received data")
			conn.send(b"Recevied \n")

	except Exception as e:
		print(str(e))
	finally:
	    	conn.close()
    		print ('Client disconnected')



def send_message(android_ip,msg_to_app,timeout = 15):

    # Insert IP and Port No. of Android device here. 
    HOST = android_ip
    PORT = 9002

    try:
	if (timeout == 0):
		print("Timeout Sending Data")
		return False
    	# Creating socket connection. 
    	client = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
    	client.connect((HOST, PORT))

    	# Converted to bytes type
    	msg_to_app = msg_to_app.encode()
    	client.sendall(msg_to_app)

    	# Receive msg from server(app).
    	msg_from_app = client.recv(1024)
    	# Decode from byte to String type
    	msg_from_app = msg_from_app.decode()
    	print ("1)", msg_from_app)

	client.close()
	return True

    except socket.error as e:
    	print("Connection does not exist")
    	print(str(e))
        time.sleep(1)
        timeout -= 1
        return send_message(android_ip,msg_to_app,timeout)
