import argparse


def file2List(fileName):
    with open(fileName, 'r') as f:
        lines = f.readlines()
    lines = [x.strip() for x in lines]
    return lines


def line2Line(line):
    columns = line.split("\t")
    newColumns = [columns[3], columns[7], columns[4], columns[5], columns[0], columns[1], columns[2], columns[11],
                  columns[6]]
    line = "\t".join(newColumns)
    return line


def list2File(fileName, list):
    with open(fileName, 'w') as f:
        f.write("\n".join(list))


parser = argparse.ArgumentParser()
args = parser.parse_args()

lines = file2List("indel.txt")
newLines = []
newLines.append("\t".join(
    ["Sample Sequence ID", "InDel type", "Sample InDel start", "Sample InDel end", "Reference sequence ID", "Ref start",
     "Ref end", "InDel sequence", "Lastz alignment strand"]))
tmpLines = map(line2Line, lines)
newLines = newLines + tmpLines
list2File("indel_out.txt", newLines)
