PhotCat
=======

This is the photocat cataloging application developed by the 
Indiana University Digital Library Program to catalog photographs
and other materials in their Fedora repository.

Building
--------

The current build scripts build a dummy version of the application that
has all local/embedded implementations.  Its only purpose is to demonstrate
the application running locally.

To perform this demonstration:

1. Run the war ant task
`ant war`
2. Set the PHOTOCAT_HOME environment variable to point to the 
   "conf/dummy/repository" directory in the source tree.
3. Deploy the build war file "dist/dummy/photocat2.war" into
   Tomcat or a compatible servlet container.

License
-------

 > Copyright 2011, Trustees of Indiana University
 > All rights reserved.
 >
 > Redistribution and use in source and binary forms, with or without
 > modification, are permitted provided that the following conditions are met:
 >
 >   Redistributions of source code must retain the above copyright notice,
 >   this list of conditions and the following disclaimer.
 >  
 >   Redistributions in binary form must reproduce the above copyright notice,
 >   this list of conditions and the following disclaimer in the documentation
 >   and/or other materials provided with the distribution.
 >  
 >   Neither the name of Indiana University nor the names of its
 >   contributors may be used to endorse or promote products derived from this
 >   software without specific prior written permission.
 >  
 > THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 > AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 > IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 > ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE
 > LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 > CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 > SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 > INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 > CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 > ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 > POSSIBILITY OF SUCH DAMAGE. 