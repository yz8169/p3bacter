import argparse


def file2List(fileName):
    with open(fileName, 'r') as f:
        lines = f.readlines()
    lines = [x.strip() for x in lines]
    return lines


def line2Line(line):
    columns = line.split("\t")
    newColumns = [columns[0] + "_" + columns[1], columns[2], columns[5] + "_" + columns[6], columns[3]]
    line = "\t".join(newColumns)
    return line


def list2File(fileName, list):
    with open(fileName, 'w') as f:
        f.write("\n".join(list))


parser = argparse.ArgumentParser()
parser.add_argument("-q", type=str, default="", help="query name")
args = parser.parse_args()

lines = file2List("snp.txt")
newLines = []
newLines.append("\t".join(["", "ref", args.q + ".site", args.q + ".base"]))
tmpLines = map(line2Line, lines)
newLines = newLines + tmpLines
list2File("snp_out.txt", newLines)
