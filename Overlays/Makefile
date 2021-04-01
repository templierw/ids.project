SRCDIR = src
OUTDIR = out
LIBDIR = lib

JC = javac
CP = -cp lib/amqp-client-5.8.0.jar

rwildcard=$(foreach d,$(wildcard $(1:=/*)),$(call rwildcard,$d,$2) $(filter $(subst *,%,$2),$d))

SRC_CHAT = $(call rwildcard,$(SRCDIR)/chat,*.java)
CLASSES_CHAT = $(SRC_CHAT:$(SRCDIR)/chat/%.java=$(OUTDIR)/chat/%.class)

SRC_RING = $(call rwildcard,$(SRCDIR)/ring,*.java)
CLASSES_RING = $(SRC_RING:$(SRCDIR)/ring/%.java=$(OUTDIR)/ring/%.class)

SRC_ELEC = $(call rwildcard,$(SRCDIR)/election,*.java)
CLASSES_ELEC = $(SRC_ELEC:$(SRCDIR)/election/%.java=$(OUTDIR)/election/%.class)

SRC_TREE = $(call rwildcard,$(SRCDIR)/tree,*.java)
CLASSES_TREE = $(SRC_TREE:$(SRCDIR)/tree/%.java=$(OUTDIR)/tree/%.class)

build: chat.jar ring.jar election.jar tree.jar

chat.jar: $(CLASSES_CHAT)
	jar cfm $@ manifest/chat.txt -C out ./chat/

$(CLASSES_CHAT): $(SRC_CHAT)
	$(JC) $(CP) $(SRC_CHAT) -d out

ring.jar: $(CLASSES_RING)
	jar cfm $@ manifest/ring.txt -C out ./ring/

$(CLASSES_RING): $(SRC_RING)
	$(JC) $(CP) $(SRC_RING) -d out

election.jar: $(CLASSES_ELEC)
	jar cfm $@ manifest/election.txt -C out ./election/

$(CLASSES_ELEC): $(SRC_ELEC)
	$(JC) $(CP) $(SRC_ELEC) -d out

tree.jar: $(CLASSES_TREE)
	jar cfm $@ manifest/tree.txt -C out ./tree/

$(CLASSES_TREE): $(SRC_TREE)
	$(JC) $(CP) $(SRC_TREE) -d out

.PHONY: clean
clean:
	rm -rf ./out/**
	rm -f *.jar