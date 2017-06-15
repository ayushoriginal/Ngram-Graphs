from Bio import SeqIO

class SequenceReader:
    
    def __init__(self):
        self._sequence_dictionary={}
    
    def read(self,input_file):
        fasta_sequences = SeqIO.parse(open(input_file),'fasta')
        for fasta in fasta_sequences:
            name, sequence = fasta.id, str(fasta.seq)
            self._sequence_dictionary[name] = sequence
                
    def getDictionary(self):
        return self._sequence_dictionary
    



