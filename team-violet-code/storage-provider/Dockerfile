
# Comments in Dockerfiles
FROM python:3.7

RUN apt-get -y update
RUN apt-get -y upgrade
# If you add the requirements.txt file before your other code, and run the pip install step right after it
# , both will only be executed if the file changes. Not on every build.
# https://vsupalov.com/speed-up-python-docker-image-build/
ADD ./src/requirements.txt /code/requirements.txt

# Update and install dependencies
RUN make && pip3 install -r /code/requirements.txt

ADD ./src /code
WORKDIR /code

EXPOSE 5000
# Run the application
CMD FLASK_APP=main.py flask run --host 0.0.0.0