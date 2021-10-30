import random
from queue import Queue

import cocotb
from cocotb.result import TestFailure
from cocotb.triggers import RisingEdge, Timer

from cocotblib.misc import simulationSpeedPrinter, ClockDomainAsyncReset

queue = Queue()
#@cocotb.coroutine
async def genClockAndReset(dut):
    dut.reset <= 1
    dut.clk <= 0
    await Timer(1000)
    dut.reset <= 0
    await Timer(1000)
    while True:
        dut.clk <= 1
        await Timer(500)
        dut.clk <= 0
        await Timer(500)


#@cocotb.coroutine
async def driverAgent(dut):
    dut.io_push_valid <= 0
    dut.io_pop_ready  <= 0

    while True:
        await RisingEdge(dut.clk)
        # TODO generate random stimulus on the hardware
        dut.io_push_valid <= random.getrandbits(1)#choice([True, False])
        randomPayload = random.randint(0, 255) # Return a random integer N such that a <= N <= b
        dut.io_push_payload <= randomPayload
        dut.io_pop_ready <= random.getrandbits(1)

#@cocotb.coroutine
async def checkerAgent(dut):
    queue = Queue()
    matchCounter = 0
    while matchCounter < 5000:
        await RisingEdge(dut.clk)
        # TODO Capture and store 'push' transactions into the queue
        if int(dut.io_push_valid) and int(dut.io_push_ready):
            queue.put(int(dut.io_push_payload))

        # TODO capture and check 'pop' transactions with the head of the queue.
        # If match increment matchCounter else throw error
        if int(dut.io_pop_valid) and int(dut.io_pop_ready):
            popVal = int(dut.io_pop_payload)
            assert not queue.empty(), f"queue should not be empty, it should pop payload={popVal}"
            refVal = queue.get()
            assert refVal == int(dut.io_pop_payload), f"queue pop payload={refVal} not match FIFO pop payload={popVal}"
            matchCounter += 1


@cocotb.test()
async def test1(dut):
    # Create all threads

    #cocotb.fork(ClockDomainAsyncReset(dut.clk,dut.reset))
    #cocotb.fork(simulationSpeedPrinter(dut.clk))
    cocotb.fork(genClockAndReset(dut))

    cocotb.fork(driverAgent(dut))
    checker = cocotb.fork(checkerAgent(dut))

    # Wait until the checker finish his job
    await checker.join()
