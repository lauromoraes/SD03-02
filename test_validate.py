import time
import struct

separator = "-"
separatorSize = 70

class Validator():
    log_file = None
    outputFiles = None
    def __init__(self, outputFiles, log_file = None):
        self.outputFiles = outputFiles
        self.log_file = log_file
        if self.log_file:
            self.log_file = open(log_file, "w")

    def __dell__(self):
        self.log_file.close()

    def print_log(self, text):
        if not isinstance(text, str):
            text = repr(text)
        print(text)
        if self.log_file:
            self.log_file.write(text + "\n")

    def validate(self):
        self.print_log("\n" + separator*separatorSize + "\n")

        mins = []
        maxs = []
        for fName in self.outputFiles:
            self.print_log("Validating all the stuff on " + fName + "...")

            f = open(fName, "rb")
            valid = True
            try:
                actual = struct.unpack('>i', f.read(4))[0]
                fileMin = actual
                next = f.read(4)
                while next != "":
                    next = struct.unpack('>i', next)[0]
                    if next < actual:
                        valid = False
                        break
                    actual = next
                    next = f.read(4)
            except:
                self.print_log("Problems to read the stuff.")
                valid = False
            finally:
                f.close()

            if valid:
                mins.append(fileMin)
                maxs.append(actual)
                self.print_log("Stuff sorted!")
                self.print_log("Min = " + repr(mins[-1]))
                self.print_log("Max = " + repr(maxs[-1]))
            else:
                self.print_log("Stuff not sorted!")
                self.print_log("\n" + separator*separatorSize + "\n")
                break

            self.print_log("\n" + separator*separatorSize + "\n")

        if valid:
            for i in range(len(mins)-1):
                if maxs[i] > mins[i+1]:
                    valid = False
                    break

        if valid:
            self.print_log("Stuff sorted on files!")
        else:
            self.print_log("Stuff not sorted on files!")

        self.print_log("\n" + separator*separatorSize + "\n")

if __name__ == '__main__':

    log_file_name = "validate_log"

    outputFiles = ["output.bin"]                    # entradas validas nesta ordem
    # outputFiles = ["result", "result2", "result3"]          # arquivo result tem o max maior que o min de result2
    # outputFiles = ["result", "result2", "result3", "teste"] # arquivo teste eh o original, nao esta ordenado

    val = Validator(outputFiles, log_file_name)
    val.validate()