SRCDIR = src
OUTDIR = out
LIBDIR = lib

JC = javac
CP = -cp lib/amqp-client-5.8.0.jar

rwildcard=$(foreach d,$(wildcard $(1:=/*)),$(call rwildcard,$d,$2) $(filter $(subst *,%,$2),$d))

SRC_OVERLAY = $(call rwildcard,$(SRCDIR)/overlay,*.java)
CLASSES_OVERLAY = $(SRC_OVERLAY:$(SRCDIR)/overlay/%.java=$(OUTDIR)/overlay/%.class)

build: node.jar

node.jar: $(CLASSES_OVERLAY)
	jar cfm $@ manifest/overlay.txt -C out ./overlay/

$(CLASSES_OVERLAY): $(SRC_OVERLAY)
	$(JC) $(CP) $(SRC_OVERLAY) -d out

.PHONY: clean
clean:
	rm -rf ./out/**
	rm -f *.jar