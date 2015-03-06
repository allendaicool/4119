JFLAGS = -g
JC = javac
.SUFFIXES: .java .class
.java.class:
	$(JC) $(JFLAGS) $*.java

CLASSES = \
       	Authenticate.java\
	client_chat_room.java\
        client_socket.java\
        Client.java\
        server_handler.java\
	Server.java\
	IP_port_tuple.java\
	client_server_socket.java\
	client_heart_beat.java

default: classes

classes: $(CLASSES:.java=.class)

clean:
	$(RM) *.class
