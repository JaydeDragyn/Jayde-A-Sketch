###                             Jayde-A-Sketch
###                             by Jayde Dragyn 
                         (jaydedragyn@outlook.com)

A single-point drawing program that emulates the Etch-A-Sketch, but with your
mouse.  Like the toy, the drawing point never leaves the "surface", so any
thing you draw is a single, continuous line.  You can erase the screen by
shaking it (space bar), get help with F1 and quit with ESC.  

This toy program will not save any images, so if you spend time creating
something, take a picture with something else or you lose it forever!

--- 

This was written as a learning exercise - I have been programming in Java for
ages (decades), but never delved into graphics.  I want to write some graphics
things (couple games, couple cellular-automata toys, maybe more), so I started
learning Swing/AWT, and this is the first completely interactive program I've
written.  It's a single, monolithic source file, but for what it is, it doesn't
really need more.  

It is feature complete as far as I care, even if I have some ideas for how to
make it "better".  I just felt like sharing it with anyone who might find it
interesting.  Constructive criticism is always welcome, but if you come here
to tell me that I've broken OOP principles or violated "proper Java convention"
or whatever, I know already.  

This is my first complete concept program in Java using AWT/Swing.  I have
written a few dozen little toy programs before this one that helped me learn 
to interpret keystrokes, mouse input, off-screen buffering, etc., but nothing 
that actually does anything that anyone would find useful, or that I really 
want to share.

I wrote my TileGame program after this one, but I didn't decide to use GitHub
until I wrote the TileGame.  I'm working on another program currently, and
decided to take a break and go back through old programs and put them on 
GitHub so you can laugh at how simple I am.  ;)

---

This was written in Java 17 (OpenJDK 17)

Developed with IntelliJ IDEA 2022.2.2(Community Edition)

The Help Screen image was created in Gimp.

---

To compile, navigate to the directory that the JaydeASketch.java file is
in and use:

    javac JaydeASketch.java
    
I like to keep my class files out of the src directory, so I use:

    javac -d ../bin/ JaydeASketch.java
    
You need to make sure the HelpScreen-v1.3.png file is in the same directory
as the class files, and then go into the directory with those class files
and use:

    java JaydeASketch
    
I have everything in a Jayde-A-Sketch directory as follows:

    Jayde-A-Sketch
        \src
            \JaydeASketch.java
        \bin
            \*.class
            \HelpScreen-v1.3.png
        \Readme.MD
        \License.MD
        
Then to compile, I go into \src and use the second command above.
I also wrote a batch file for windows that goes into the \bin directory and
uses

    start javaw JaydeASketch
    
(javaw so it does not open a console window) so I can then double-click it
from Windows file explorer.
You could create a shell script in linux to do the same thing.  
I don't know the first thing about the Mac environment, so you're on your 
own there.
    
