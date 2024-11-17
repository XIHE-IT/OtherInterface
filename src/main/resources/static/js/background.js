export function myBackground() {
	const div = document.createElement('div')
	div.id = 'my-module'
	div.innerHTML = `
         <footer>
                <div class="footer-model">
                    <div>
                        <span class="sp-font1">联系热线</span>
                        <span class="sp-font2">上海品客国际物流有限公司</span>
                    </div>
                    <div>
                        <span class="sp-font3">400-086-1778</span>
                        <span class="sp-font4">Shanghai Pinke International Logistics Co. Ltd</span>
                    </div>
                </div>
            </footer>
    `
	return div
}

export function fuelDate() {
	return {
		code: 200,
		msg: 'OK',
		data: [
			{
				rateTypeINfo: 'UPS',
				regionList: [
					{
						regionInfo: '美国',
						rateList: [
							{
								communs1: '22.50%',
								communs2: '26.25%',
								communs3: '16.75%',
								communs4: '16.00%',
								communs5: '19.75%',
								ratetime: '24/10/21'
							},
							{
								communs1: '22.50%',
								communs2: '26.25%',
								communs3: '16.50%',
								communs4: '16.00%',
								communs5: '19.50%',
								ratetime: '24/10/14'
							}
						]
					},
					{
						regionInfo: '中国',
						rateList: [
							{
								communs1: '28.25%',
								communs2: '28.25%',
								communs3: '',
								communs4: '',
								ratetime: '24/10/21'
							},
							{
								communs1: '28.25%',
								communs2: '28.25%',
								communs3: '',
								communs4: '',
								ratetime: '24/10/14'
							}
						]
					},
					{
						regionInfo: '香港',
						rateList: [
							{
								communs1: '28.25%',
								communs2: '28.25%',
								communs3: '',
								communs4: '',
								ratetime: '24/10/21'
							},
							{
								communs1: '28.25%',
								communs2: '28.25%',
								communs3: '',
								communs4: '',
								ratetime: '24/10/14'
							}
						]
					}
				]
			},
			{
				rateTypeINfo: 'FED',
				regionList: [
					{
						regionInfo: '美国',
						rateList: [
							{
								communs1: '23.00%',
								communs2: '26.75%',
								communs3: '16.25%',
								communs4: '16.25%',
								ratetime: '24/10/21'
							},
							{
								communs1: '22.50%',
								communs2: '26.25%',
								communs3: '16.25%',
								communs4: '16.00%',
								ratetime: '24/10/14'
							}
						]
					},
					{
						regionInfo: '中国',
						rateList: [
							{
								communs1: '27.50%',
								communs2: '27.50%',
								communs3: '',
								communs4: '',
								ratetime: '24/10/28'
							},
							{
								communs1: '28.75%',
								communs2: '28.75%',
								communs3: '',
								communs4: '',
								ratetime: '24/10/21'
							}
						]
					},
					{
						regionInfo: '香港',
						rateList: [
							{
								communs1: '27.50%',
								communs2: '27.50%',
								communs3: '',
								communs4: '',
								ratetime: '24/10/28'
							},
							{
								communs1: '28.75%',
								communs2: '28.75%',
								communs3: '',
								communs4: '',
								ratetime: '24/10/21'
							}
						]
					}
				]
			},
			{
				rateTypeINfo: 'DHL',
				regionList: [
					{
						regionInfo: '美国',
						rateList: [
							{
								communs1: '16.50%',
								communs2: '20.25%',
								communs3: '',
								communs4: '',
								ratetime: '24/10/01'
							},
							{
								communs1: '18.00%',
								communs2: '21.75%',
								communs3: '',
								communs4: '',
								ratetime: '24/09/01'
							}
						]
					},
					{
						regionInfo: '中国',
						rateList: [
							{
								communs1: '24.50%',
								communs2: '24.50%',
								communs3: '',
								communs4: '',
								ratetime: '24/10/01'
							},
							{
								communs1: '26.50%',
								communs2: '26.50%',
								communs3: '',
								communs4: '',
								ratetime: '24/09/01'
							}
						]
					},
					{
						regionInfo: '香港',
						rateList: [
							{
								communs1: '24.50%',
								communs2: '24.50%',
								communs3: '',
								communs4: '',
								ratetime: '24/10/01'
							},
							{
								communs1: '26.50%',
								communs2: '26.50%',
								communs3: '',
								communs4: '',
								ratetime: '24/09/01'
							}
						]
					}
				]
			}
		]
	}
}
