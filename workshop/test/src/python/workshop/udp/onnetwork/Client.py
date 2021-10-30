import socket
import sys
import threading

import time

from _socket import SOL_SOCKET, SO_REUSEADDR, SO_BROADCAST

RX_IP = open('ip.txt', 'r').read()
TX_IP = "255.255.255.255"
SERVER_PORT = 37984

sock = socket.socket(socket.AF_INET, # Internet
                     socket.SOCK_DGRAM) # UDP
sock.setsockopt(SOL_SOCKET, SO_REUSEADDR, 1)
sock.setsockopt(SOL_SOCKET, SO_BROADCAST, 1)
sock.bind((RX_IP, 0))

def rxThread(sock,dummy):
    # while True:
    data, addr = sock.recvfrom(2048)
    print("received message:", data, addr)

t = None
try:
    t = threading.Thread(target = rxThread, args = (sock, 1))
    t.daemon = True
    t.start()
except Exception as errtxt:
    print(errtxt)

print("Send request")
sock.sendto(chr(0x11).encode(), (TX_IP, SERVER_PORT))
print("Wait two seconds for answers")


if t is None:
    sys.exit("Failed to create receive thread")
t.join(timeout = 2.0)
if t.is_alive():
    sys.exit("No message received after timeout")
print("Done")
