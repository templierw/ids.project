SRCDIR = src
OUTDIR = out
LIBDIR = lib

JC = javac
CP = -cp lib/amqp-client-5.8.0.jar

rwildcard=$(foreach d,$(wildcard $(1:=/*)),$(call rwildcard,$d,$2) $(filter $(subst *,%,$2),$d))

SRC_OVERLAYS = $(call rwildcard,$(SRCDIR)/overlays,*.java)
CLASSES_OVERLAYS = $(SRC_OVERLAYS:$(SRCDIR)/overlays/%.java=$(OUTDIR)/overlays/%.class)

build: node.jar

node.jar: $(CLASSES_OVERLAYS)
	jar cfm $@ manifest/overlays.txt -C out ./overlays/

$(CLASSES_OVERLAYS): $(SRC_OVERLAYS)
	$(JC) $(CP) $(SRC_OVERLAYS) -d out

.PHONY: clean
clean:
	rm -rf ./out/**
	rm -f *.jar