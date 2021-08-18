FROM python:3.7.5-buster

ENV PYTHONUNBUFFERED 1

RUN apt-get update && apt-get install -y --no-install-recommends \
                bison \
                flex \
        && rm -rf /var/lib/apt/lists/*

ENV LIBRARY_PATH /usr/local/lib
ENV LD_LIBRARY_PATH /usr/local/lib
ENV LIBRARY_INCLUDE_PATH /usr/local/include

# PBC
COPY --from=initc3/pbc:0.5.14-buster \
                /usr/local/include/pbc \
                /usr/local/include/pbc
COPY --from=initc3/pbc:0.5.14-buster \
                /usr/local/lib/libpbc.so.1.0.0  \
                /usr/local/lib/libpbc.so.1.0.0
RUN set -ex \
    && cd /usr/local/lib \
    && ln -s libpbc.so.1.0.0 libpbc.so \
    && ln -s libpbc.so.1.0.0 libpbc.so.1

# Setup virtualenv
ENV PYTHON_LIBRARY_PATH /opt/venv
ENV PATH ${PYTHON_LIBRARY_PATH}/bin:${PATH}

# Install charm
# Creates /charm/dist/Charm_Crypto...x86_64.egg, which gets copied into the venv
# /opt/venv/lib/python3.7/site-packages/Charm_crypto...x86_64.egg
RUN set -ex \
        \
        && mkdir -p /usr/src/charm \
        && git clone https://github.com/JHUISI/charm.git /usr/src/charm \
        && cd /usr/src/charm \
        && python -m venv ${PYTHON_LIBRARY_PATH} \
        && ./configure.sh \
        && make install \
        && rm -rf /usr/src/charm
# If you add the requirements.txt file before your other code, and run the pip install step right after it
# , both will only be executed if the file changes. Not on every build.
# https://vsupalov.com/speed-up-python-docker-image-build/
ADD ./src/requirements.txt /code/requirements.txt
ADD ./src/setup.py /code/setup.py

# RUN apt-get -y install flex bison libssl-dev python-dev libgmp-dev
# # RUN wget http://crypto.stanford.edu/pbc/files/pbc-0.5.14.tar.gz && tar xf pbc-0.5.14.tar.gz && cd pbc-0.5.14
# # RUN ./configure && make && sudo make install
# ENV PBC_VERSION=0.5.14
# RUN /bin/sh -c set -ex                 && wget -O pbc.tar.gz "https://crypto.stanford.edu/pbc/files/pbc-$PBC_VERSION.tar.gz"         && mkdir -p /usr/src/pbc         && tar -xzC /usr/src/pbc --strip-components=1 -f pbc.tar.gz         && rm pbc.tar.gz 	    	    && cd /usr/src/pbc         && ./configure         && make         && make install         && rm -rf /usr/src/pbc#
# Update and install dependencies
RUN pip3 install -r /code/requirements.txt
# RUN $(RM) -r pox3.egg-info/ dist/ build/
# RUN python3 /code/setup.py sdist bdist_wheel

ADD ./src /code
WORKDIR /code

EXPOSE 5000
# Run the application
CMD FLASK_APP=main.py flask run --host 0.0.0.0