package main

import "fmt"
import "bufio"
import "os"
import "github.com/iotaledger/giota"
import "time"
import "strconv"

func main() {
	const maxTimestampTrytes = "MMMMMMMMM"
	const TimestampTrinarySize = 27

	scanner := bufio.NewScanner(os.Stdin)
	scanner.Scan()
	giota.PowProcs, _ = strconv.Atoi(scanner.Text());

	powName, pow := giota.GetBestPoW()
	fmt.Println(powName)

	for true {
		scanner.Scan()
		input := scanner.Text()
		trytes, _ := giota.ToTrytes(input)
		tx, _ := giota.NewTransaction(trytes)

		timestamp := giota.Int2Trits(time.Now().UnixNano()/1000000, TimestampTrinarySize).Trytes()
		tx.AttachmentTimestamp = timestamp
		tx.AttachmentTimestampLowerBound = ""
		tx.AttachmentTimestampUpperBound = maxTimestampTrytes

		tx.Nonce, _ = pow(tx.Trytes(), int(14))

		fmt.Println(tx.Trytes())
	}
}
