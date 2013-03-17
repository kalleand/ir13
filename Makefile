.PHONY: build, run, irun, drun, clean, brun, move, buildPagerank, runPagerank
default: build

ifndef FILES
FILES=1000
endif
ifndef MODE
MODE=d
endif

build:
	javac -Xlint:none -cp .:pdfbox:megamap -d . src/*.java

run: build
ifeq ($(MODE), d)
	java -Xmx1024m -cp .:pdfbox:megamap ir.SearchGUI -$(MODE) svwiki/files/$(FILES) -m
else
	java -Xmx1024m -cp .:pdfbox:megamap ir.SearchGUI -$(MODE) $(FILES) -m
endif

irun: build
	java -Xmx1024m -cp .:pdfbox:megamap ir.SearchGUI -i $(FILES) -m

drun:
	java -Xmx1024m -cp .:pdfbox:megamap ir.SearchGUI -d svwiki/files/$(FILES) -m

clean:
	rm -f ir/*.class
	rm -f index_*

brun: build
	java -Xmx1024m -cp .:pdfbox:megamap ir.SearchGUI -d svwiki/files/$(FILES) -b

move:
ifdef INDEX
	mv $(INDEX).index $(FILES).index
	mv $(INDEX).data $(FILES).data
endif

buildPagerank:
	javac pagerank/PageRank.java

runPagerank:
ifeq ($(FILES), all)
	java pagerank.PageRank svwiki_links/links.txt
else
	java pagerank.PageRank svwiki_links/links$(FILES).txt
endif
