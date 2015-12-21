# Command line utility to extract multipart form data

The problem: You have used Wireshark to capture a network packet trace 
of an HTML form submission over HTTP between client and server. The HTML
form submission uses multipart/form-data encoding (RFC 1867) to safely
include binary file uploads. You need to extract the contents of the uploaded
files as well as any text fields in the form.

The solution: This utility.

This code uses the MultipartStream class from the Apache FileUpload 
project to extract any field and files uploaded in "multipart/form-data" 
format.

This software is released under the Apache 2.0 Licence.
