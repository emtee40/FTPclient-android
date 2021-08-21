# Use this script in /usr/share/mime/application/
import os

files = os.listdir(".")
mimetypesList = []
for i in files:
    file = open(i, "r")
    lines = file.readlines()
    file.close()
    icon = "No icon"
    mime = []
    for line in lines:
        if line.strip().startswith("<generic-icon name="):
            start = line.find('"') + 1
            line = line[start:]
            end = line.find('"')
            icon = line[:end]
        if line.strip().startswith("<glob pattern="):
            patternStart = line.find('.') + 1
            line = line[patternStart:]
            patternEnd = line.find('"')
            pattern = line[:patternEnd]
            mime.append(pattern)

    inList = False
    for iconList in mimetypesList:
        if iconList[0] == icon:
            inList = True
            for pattern in mime:
                iconList.append(pattern)

    if not inList:
        mimes = [icon]
        for pattern in mime:
            mimes.append(pattern)
        mimetypesList.append(mimes)

for i in mimetypesList:
    for l in i:
        if not i.index(l) == 0:
            print('"' + l + '"', end=", ")
        else:
            print(l, end=": ")
    print("\n")

print("")
