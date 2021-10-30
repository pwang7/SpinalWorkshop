import random

import cocotb
from cocotb.result import TestFailure
from cocotb.triggers import RisingEdge, Timer


# @cocotb.coroutine
async def genClockAndReset(dut):
    dut.reset <= 1
    dut.clk   <= 0
    # yield Timer(1000)
    await Timer(500, units="ns")
    # TODO Animate the dut.clk and dut.reset
    dut.reset <= 0
    while True:
        dut.clk <= 0
        await Timer(1, units="ns")
        dut.clk <= 1
        await Timer(1, units="ns")

@cocotb.test()
async def test1(dut):
    bit_width = dut.io_value.value.n_bits
    max_value = 2 ** bit_width

    clear = False
    dut.io_clear <= clear
    cocotb.fork(genClockAndReset(dut))

    counter = 0  # Used to model the hardware
    for i in range(128):
        # yield RisingEdge(dut.clk)
        await RisingEdge(dut.clk)
        # TODO Check that the DUT match with the model (counter variable)
        # read io_value =>     dut.io_value
        # read io_full =>      dut.io_full
        # raise TestFailure("io_value missmatch")
        # raise TestFailure("io_full missmatch")
        io_value = int(dut.io_value)
        io_full = int(dut.io_full)
        full = (counter == max_value - 1)
        # print(f"io_value={io_value}, io_full={io_full}, counter={counter}, full={full}")
        assert io_value == counter, f"io_value={io_value} mismatch counter={counter}"
        assert io_full == full, f"io_full={io_full} mismatch full={full}"

        # TODO Animate the model depending DUT inputs
        counter = (counter + 1) % max_value

        # TODO Generate random stimulus
        if i % 25 == 0:
            clear = random.choice([True, False])
            dut.io_clear <= clear
            # print(f"clear={clear}")
            if clear:
                await RisingEdge(dut.clk)
                dut.io_clear <= False
                counter = 0
