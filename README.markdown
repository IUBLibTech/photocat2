PhotoCat
========

This is the photocat cataloging application developed by the 
Indiana University Digital Library Program to catalog photographs
and other materials in their Fedora repository.

# Key Features

1. Cataloger-Centered Design
   The whole purpose of the application is to make the often tedious 
   work of cataloging easier so that catalogers can apply their 
   expertise quickly without wasting time with tedious interfaces.
   
   To this end, Photocat supports auto-complete text entry that can
   be linked to internal and external sources.  Vocabularies can 
   be managed locally or externally.  
   
   It supports sophisticated reporting and search and replace
   functionality.
   
   Support for spreadsheet-based export and import enable yet
   another convenient way to manage metadata.
   
2. Configurable Metadata
   Photocat was written with the knowledge that metadata standards
   and best practices change constantly.  
   
   Towards that end, every collection has its own metadata configuration
   that defines what fields are available, how they're labeled, what
   usage notes should be made available to catalogers, how the field
   should show up in search results, etc.  Furthermore individual fields
   may be designated as private and are protected from access by 
   unauthorized individuals.
   
   Which metadata fields are exposed in the cataloging interface for
   a given collection is something that can be configured and changed
   at any time.
   
   Collection configuration includes the ability to export metadata to
   alternate formats.
   
3. Modular architecture
   The Photocat application is only a piece of a larger repository 
   environment.  While this codebase includes an indexing service,
   search service and recommended repository service, those pieces
   may be substituted by conforming to a clear and documented API.
   
   With relative ease, one could use an alternate search server,
   different storage repository or derivative generation mechanism.
   
4. Preservation-focused
   Photocat was written knowing that descriptive metadata is a
   valuable resource, whose preservation is of the utmost importance. 
   - Full history and audit trail of changes is preserved
   - Repository-centric design (all metadata is stored in the repository)
   - documented formats for transparency


# Overview

The application requires several supporting applications in order to
function.

## Fedora
Fedora must be installed and must have the resource index enabled with
synchronous updates.  JMS messaging must also be turned on. Furthermore,
XACML policy enforcement is used by Photocat, so Fedora need not be
exclusively used for photocat, and may be the host repository for other
applications. 

## fedora-index-service
This simple application listens to JMS messages emitted by fedora to 
maintain a search index used by SRW.

## SRW 
This extension of the OCLC SRW server implementation exposes a 
sophisticated search API used by Photocat.

## Photocat
This is the cataloging application, and public facing discovery
application.  One can configure Photocat to only expose the cataloging 
interface, the public discovery interface or have a single application
provide both.  This allows for a variety of deployment configurations
to suit various needs.  The application is built with concurrency in 
mind and multiple instances running at once all pointing at the same
repository is a fully supported use case.


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
