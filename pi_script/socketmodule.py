#!/usr/bin/env python3
 
import socket
import ifaddr
import _thread


def startServer(ip_address_pi):

    HOST = str(ip_address_pi)
    PORT = 9008


    server = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
    server.bind((HOST, PORT))
    server.listen()
    while True:
        conn, addr = server.accept()
        conn.send(b"Good I am Server ready to receive your messages:)\n")
        msg_from_app = ''
        while True:
            # Receiving data in form of chunks of size 4096 (can be changed as per usage).
            msg_chunk = conn.recv(4096)
            # Data is receieved in the form of bytes decode it into String type.
            msg_chunk = msg_chunk.decode()
            if not msg_chunk: 
                break
            msg_from_app += msg_chunk
            print (msg_from_app)
        
        conn.close()
        print ('Client disconnected')





<<<<<<< HEAD
def startClient(IP,msg_to_app):

    # Insert IP and Port No. of Android device here. 
    HOST = IP
=======
def startClient(msg_to_app):

    # Insert IP and Port No. of Android device here. 
    HOST = "10.27.145.226"
>>>>>>> 5eacd1300a02be466b7bbb42fb99c39e20185020
    PORT = 9002

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

    # On Sending "Talk" message to app it will return "Okay". 
    if(msg_from_app=="Okay\n"):
        client.close()
        
        # Finding the WI-FI Adapter ip address
        adapters = ifaddr.get_adapters()
        ip_address_pi = ''
        for adapter in adapters:
            if("Wi-Fi Adapter" in adapter.nice_name):
                for ip in adapter.ips:
                    ip_address_pi = ip.ip
                    print ("%s" % (ip.ip))
                    break
        
        ip_addr_pi = ip_address_pi + "\n"            
        # Send pi ip address to the app
        startClient(ip_addr_pi)
        _thread.start_new_thread(startServer(ip_address_pi))


startClient("Hello\n")