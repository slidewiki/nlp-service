Problem Statement/Task: 
Given a set of slides and a set of questions related to the slide contents, we need to find the answer of the question 
or a reference in the slide where the answer could possibly be found.

Proposed Idea: 
We frame a pair of line wise slide contents and question combinations. 
Now the probability score for the similarity of both pairs will give the probable answer.

Technical Solution 1:
We use neural network (bi-lstm with attentive matching) to match the similarities between sentences based on
(https://github.com/galsang/BIMPM-pytorch/blob/master/model/BIMPM.py).
The model is trained on Simple Questions set using Glove embedding and then it is used to predict similarity probability.

Example Slide used for test predictions: https://slidewiki.org/print/90725/_/90725/
Actual test file (SW_test.txt) is attached.

Results:
Validation accuracy is 81.83%
Results file (SW_test_with_scores.txt) is attached giving high probability for slide which is having the answer to given question.



Technical Solution 2:
We use transformers(BERT) and fine tune it on SQuAD for question-answering (on SQuAD datasets).
https://github.com/huggingface/transformers

Results:
Accuracy on validation set is 84.4
