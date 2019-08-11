
jas5: project idea, RISC-V assembler, not taking a .S input file but as a Java library that takes its inputs
from method calls:
    Segment text = assembler.getSegment("text");
    text.addi(s0, t0, 5); // s0, t0 from static imports
    Label skipAddi = text.label();
    // ...
    // TODO should labels have a global name?
    // The line below has problems because we cannot use a label before we place it.
    assembler.getSegment("data").label("myGlobalVar").word(99);

    // alternative to above named and unnamed segments: unnamed *yet-unplaced* segments
    // It is an error to place a segment multiple times or to use a segment which at the end of
    // compilation is still unplaced.
    Label alreadyUsedButUnplacedLabel = somewhere.label;
    assembler.getSegment("data").label(alreadyUsedButUnplacedLabel).word(99);
    
--> MaJaI not dependent on riscv-as and riscv-ld for compilation.

Downside: Have to re-write native methods the same way. That makes them less readable.


We need a final linker pass where the actual, then-known address of labels is placed in reference sites.
If we separate that (call it jld5), we have a separate linker and can link modules together. Either that or at least
a jas5-integrated linker that can produce and use a global symbol table from the "first loaded module" while
building the "second loaded module" is needed for loading modules at different times, e.g. run-time class loading.
