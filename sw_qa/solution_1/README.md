# sentence_similarity:
A neural network model is used for predicting similarity between a pair of sentences(sentence_A:slide_content, sentence_B:question) as probability\
Model used for this is bimpm(Bi-LSTM with Attention).\
We carry our training and validations on simple questions dataset. \
For training, the simple question with entity and predicates together form sentence B where the question itself is sentence A. \
Sentence A and Sentence B are both passed as inputs to the models and similarity probability between both is found out. \

# environment requirements:
--create an environment suitable for this model to run on GPU\
conda create --name py36 python=3.6 \
aconda activate py36 \
conda install -c pytorch numpy==1.14.2 \
conda install -c pytorch scikit-learn==0.19.1 \
conda install -c pytorch spacy==2.0.11 \
conda install -c conda-forge tensorboardx \
pip install downloaddir/torch-0.3.0.post4-cp36-cp36m-linux_x86_64.whl \
pip install torchtext==0.2.3 \
conda install -c pytorch ignite==0.1 \
python -m spacy download en \

Example to run program for both models with some optional adjustable parameters: \
(py36) ukumar@sda-srv04:~/sentence_similarity$ python main.py --batch-size 32 \
Validation Results - Epoch: 15 Accuracy: 0.8307 \
Test Results - Epoch: 15 Accuracy: 0.8183 \
 eg of model: \
 f4c38bd0-62d3-4a85-a3ad-d41765b12269.model \

(py36) ukumar@sda-srv04:~/sentence_similarity$ python main.py --batch-size 64 \
Validation Results - Epoch: 15 Accuracy: 0.7293 \
Test Results - Epoch: 15 Accuracy: 0.7315 \
--model can be saved so that in next run we do not have to train the model, and just can get the results. eg of model: \
0179ad48-c41c-4644-a42e-d4b502a829e7.model \

We use the model with checkpoint made using batch-size as 32 \
The results file is uploaded in folder for solution_1

