Tried BERT on SQuAD Question Answering dataset(suggested by Abbas as per implementation in https://github.com/huggingface/transformers).

Resolved configuration errors for model's implementation, resolved runtime memory errors.

Accuracy on validation set is 84.4 (screenshot attached))

Run configurations:
CUDA_VISIBLE_DEVICES=2 python -m torch.distributed.launch ./examples/question-answering/run_squad.py \
    --model_type bert \
    --model_name_or_path bert-large-uncased-whole-word-masking \
    --do_train \
    --do_eval \
    --train_file ./squad/data/train-v1.1.json \
    --predict_file ./squad/data/dev-v1.1.json \
    --learning_rate 3e-5 \
    --num_train_epochs 2 \
    --max_seq_length 96 \
    --doc_stride 64 \
    --output_dir ../models/wwm_uncased_finetuned_squad/
    
    The checkpoint saved from training using above configurations will be used for slidewiki qa predictions.
