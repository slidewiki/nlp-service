FROM openjdk:8-jre
MAINTAINER Roy Meissner <meissner@informatik.uni-leipzig.de>

#TODO convert to java Play!

#RUN mkdir /nodeApp
#WORKDIR /nodeApp

# ---------------- #
#   Installation   #
# ---------------- #

#ADD ./application/package.json ./
#RUN npm install --production

#ADD ./application/ ./

# ----------------- #
#   Configuration   #
# ----------------- #

EXPOSE 80

# ----------- #
#   Cleanup   #
# ----------- #

RUN apt-get autoremove -y && apt-get -y clean && \
		rm -rf /var/lib/apt/lists/*

# -------- #
#   Run!   #
# -------- #

#CMD npm start
