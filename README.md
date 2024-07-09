# OntoMutate

A prototype for mutation of knowledge graphs with respect to robustness masks

## Requirements
 - Java JRE and JDK
 - gradle

## Usage
 - `mutate.sh` is the main script that can be used to generate a mutant for an input knowledge graph. Run `mutate.sh -h` to see the options how to run the program. The user needs to specify the input knowledge graph and a file name for the output graph. Optionally, a mask that the output needs to conform to and the number of mutations that are applied can be provided. Note, that the script first builds the project, which only need to be done once after the source code is modified.
 - `test_system.sh` is a simple script that tests whether the source code can be compiled and does a simple mutation to evaluate if the setup is correct.

### Specifying Mutation Operators
Per default, five domain-independent mutation oprators are used. To add more (existing) mutation operators, their classes need to be added to the list in lines 108–112 in file [Main.kt](src/main/kotlin/org/smolang/robust/Main.kt). To define new mutation operators, one can define them as sub-classes of the class [Mutation](src/main/kotlin/org/smolang/robust/mutant/Mutation.kt) (see e.g. mutation operators targeting the ABox in [MutationAbox](src/main/kotlin/org/smolang/robust/mutant/MutationABox.kt)).

## Evaluation for ISSRE Publication
The mutants, masks and results of test runs can be found in the following folders:

| SUT | masks | mutants | test results |
| ----|-------|---------|----------------|
| geo | [sut/geo/masks](sut/geo/masks) | [sut/geo/mutatedOnt/ISSRE](sut/geo/mutatedOnt/ISSRE) | [sut/geo/testResults/ISSRE](sut/geo/testResults/ISSRE) |
| suave | [sut/suave/masks](sut/suave/masks) | [sut/suave/mutatedOnt/ISSRE](sut/suave/mutatedOnt/ISSRE) | [sut/suave/testResults/ISSRE](sut/suave/testResults/ISSRE) |

Each folder for the masks contains a file `mask_development.txt` that explains, how the masks where developed over time and which test cases used which masks.

## Replication of Evaluation for ISSRE Publication

The easiest way to replicate our results is to use the following [VM](https://zenodo.org/records/12699141) where all SUTs are already implemented. If you want to install everything yourself, you can find instructions on how to do so in the next subsection.

 - `generate_graph.sh` generates the graph from the paper. The PDF output is put into a folder `results`. (This script requires a LaTex installation to produce the PDF.) The run time of the script is a few minutes.
 - `replicate_geo.sh` generates the mutants for the geo system and executes the test runs for all of them. The mutants are saved in folder [sut/geo/mutatedOnt](sut/geo/mutatedOnt) and the results of the test runs in [sut/geo/testResults](sut/geo/testResults). On our machine (Intel Core i7-1165G7) this took about 100 hours.
 - `replicate_suave.sh` generates the mutants for the suave system and executes the test runs for all of them. On our machine (Intel Core i7-1165G7) this took about 60 hours.


### Installation of SUTs
You can use the script `install_suts.sh` to install all the necessary software. The last part of the script is rebooting, to make the installment permanent

#### SUAVE (docker)
 - see [website](https://docs.docker.com/engine/install/ubuntu/)
 - add to groups, e.g. following [this website](https://docs.docker.com/engine/install/linux-postinstall/)

#### Geo Simulator
- install Java, e.g. using

  - `sudo apt install default-jre`

  - `sudo apt install default-jdk`
- install [gradle](https://gradle.org/install/)
- get the geo simulator form the fork [here](https://anonymous.4open.science/r/SemanticObjects-FF2C/README.md) copy it in some `FOLDER`.
- insert the path of the cloned repository, i.e. `FOLDER/SemanticObjects`,  in the [config file](sut/geo/config.txt). (can be relative or absolute path)
- call `build_geo.sh`
