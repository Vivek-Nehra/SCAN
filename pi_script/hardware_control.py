import RPi.GPIO as GPIO
import time

GPIO.setmode(GPIO.BCM)
GPIO.setup(16,GPIO.OUT)
GPIO.output(16, GPIO.LOW)

def unlock():
    # LED ON
    GPIO.output(16,GPIO.HIGH)

def lock():
    #LED OFF
    GPIO.output(16,GPIO.LOW)

def release():
    #LED BLINK FAST
    GPIO.output(16,GPIO.HIGH)
    time.sleep(0.5)
    GPIO.output(16,GPIO.LOW)
    time.sleep(0.5)
    GPIO.output(16,GPIO.HIGH)
    time.sleep(0.5)
    GPIO.output(16,GPIO.LOW)
    time.sleep(0.5)
    GPIO.output(16,GPIO.HIGH)
    time.sleep(0.5)
    GPIO.output(16,GPIO.LOW)
    time.sleep(0.5)
    GPIO.output(16,GPIO.HIGH)
    time.sleep(0.5)
    GPIO.output(16,GPIO.LOW)
    GPIO.cleanup()


def connected():
    #LED BLINK
    GPIO.output(16,GPIO.LOW)
    time.sleep(0.5)
    GPIO.output(16,GPIO.HIGH)
    time.sleep(0.5)
    GPIO.output(16,GPIO.LOW)
    time.sleep(0.5)
    GPIO.output(16,GPIO.HIGH)
    time.sleep(0.5)
    GPIO.output(16,GPIO.LOW)
    time.sleep(0.5)
    GPIO.output(16,GPIO.HIGH)
    time.sleep(0.5)
    GPIO.output(16,GPIO.LOW)
    time.sleep(0.5)
    GPIO.output(16,GPIO.HIGH)
