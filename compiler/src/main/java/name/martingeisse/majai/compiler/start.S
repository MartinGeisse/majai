.option norvc

// helper for GP initialization, see below
.section .sdata
sdata:
.text

// startup / interrupt entry switch
    j start
    j interruptEntry

// startup logic
start:
    // Initialize the GP register in a way that fools the ld tool, disallowing link-time optimization (specifically,
    // section relaxation) so it doesn't try to use GP to initialize GP. To do that, we load an address that has a
    // distance greater than 0x800 from the assumed / intended value of GP; 0x800 is the largest possible immediate
    // offset for loads/stores and immediate constants for ADDI. Then, we correct the error at run-time.
    la gp, sdata - 1
    addi gp, gp, 2
    addi gp, gp, 0x7ff

    // initialize the SP register
    lui sp, 0x00001

    // initialize the TP register to the beginning of the dynamic heap
    li tp, dynamicHeap

    // main program
    // li a0, 7
    // call name_martingeisse_majai_payload_Test_foo_I_I
    // sw a0, -8(x0)
    call name_martingeisse_majai_payload_Test_main__V

    // quit simulator
    // sw x0, -4(x0)

    // hang in case this is real hardware
hang:
    j hang

// interrupt logic
interruptEntry:
    j interruptEntry

// --------------------------------------------------------------------------------------------------------------------
// various native methods
// --------------------------------------------------------------------------------------------------------------------

name_martingeisse_majai_payload_Test_storeWord_II_V:
    sw a1, 0(a0)
    ret

// --------------------------------------------------------------------------------------------------------------------
// memory management
// --------------------------------------------------------------------------------------------------------------------

// we are currently using the tp register as the allocation pointer

allocateMemory:
    add tp, tp, a0
    sub a0, tp, a0
    sw a1, 0(a0)
    ret

// --------------------------------------------------------------------------------------------------------------------
// generated code
// --------------------------------------------------------------------------------------------------------------------
